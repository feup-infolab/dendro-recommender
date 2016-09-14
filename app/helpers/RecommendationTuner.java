package helpers;

import com.google.common.collect.ImmutableMap;
import connections.MySQLConnection;
import models.RecommendationSettings;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.BooleanPreference;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by joaorocha on 17/05/15.
 */
public class RecommendationTuner extends Thread {

    private static boolean ACTIVE = false;

    public static final int BOOLEAN_RATING_MODE = 1;
    public static final int ABSOLUTE_FREQUENCIES_RATING_MODE = 2;
    public static final int RELATIVE_FREQUENCIES_RATING_MODE = 3;
    public static final int TF_IDF_RATING_MODE = 4;

    public static final int AVERAGE_NORMALIZATION = 1;
    public static final int GAUSSIAN_NORMALIZATION = 2;
    public static final int DECOUPLING_NORMALIZATION = 3;
    public static final int NO_NORMALIZATION = 4;

    public static final int NUMBER_OF_EVALUATION_RUNS = 45;
    public static final int TIME_BETWEEN_UPDATES = 10000;

    public static boolean isActive() {
        return ACTIVE;
    }

    private static final Map<Integer,String> ratingModesDescriptions = ImmutableMap.of(
            BOOLEAN_RATING_MODE, "Boolean Rating",
            ABSOLUTE_FREQUENCIES_RATING_MODE, "Absolute Frequencies Rating",
            RELATIVE_FREQUENCIES_RATING_MODE, "Relative Frequencies Rating",
            TF_IDF_RATING_MODE, "\"Tf-Idf\" Rating Mode"
    );

    private static final Map<Integer,String> normalizationModesDescriptions = ImmutableMap.of(
            NO_NORMALIZATION, "No normalization",
            AVERAGE_NORMALIZATION, "Normalization by user's average Rating",
            GAUSSIAN_NORMALIZATION, "Gaussian normalization",
            DECOUPLING_NORMALIZATION, "Decoupling normalization"
    );


    private static final int[] RATING_MODES = {RELATIVE_FREQUENCIES_RATING_MODE, BOOLEAN_RATING_MODE, ABSOLUTE_FREQUENCIES_RATING_MODE, TF_IDF_RATING_MODE};
    private static final int[] NORMALIZATION_TYPES = {AVERAGE_NORMALIZATION, GAUSSIAN_NORMALIZATION, DECOUPLING_NORMALIZATION, NO_NORMALIZATION};
    private boolean shuttingDown = false;

    public void shutdown()
    {
        this.shuttingDown = true;
    }

    @Override
    public void run() {
        while(!shuttingDown && isActive()) {
            double[] evaluationScores = new double[NUMBER_OF_EVALUATION_RUNS];

            try {
                RecommendationSettings lastBest = RecommendationSettings.getCurrentBest();

                //See if there are new interactions to recalculate parameters
                String sqlCount = "SELECT COUNT(*) as count FROM interactions WHERE created > ?";
                PreparedStatement countNewInteractionsStatement = MySQLConnection.prepareStatement(sqlCount);
                countNewInteractionsStatement.setTimestamp(1, lastBest.getLimitingInteractionTimestamp());
                ResultSet numberOfNewInteractionsResultSet = countNewInteractionsStatement.executeQuery();
                numberOfNewInteractionsResultSet.next();
                int numberOfNewInteractions = numberOfNewInteractionsResultSet.getInt(1);

                //System.out.println("Recalculating recommendation parameters parameters...");
                if(numberOfNewInteractions > 0)
                {
                    String sql = "SELECT * FROM interactions WHERE created > ?";
                    PreparedStatement newInteractionsStatement = MySQLConnection.prepareStatement(sql);
                    newInteractionsStatement.setTimestamp(1, lastBest.getLimitingInteractionTimestamp());
                    ResultSet newInteractions = newInteractionsStatement.executeQuery();
                    int currentInteractionIndex = 0;

                    while (newInteractions.next()) {

                        Double progress = (double) currentInteractionIndex / (double) numberOfNewInteractions;
                        //System.out.println(Precision.round(progress, 2) + "...");

                        DualHashBidiMap<String, Long> users = new DualHashBidiMap<>();
                        DualHashBidiMap<String, Long> descriptors = new DualHashBidiMap<>();

                        HashMap<String, HashMap<String, Integer>> frequencies = new HashMap<>();

                        HashMap<String, Integer> userInterCount = new HashMap<>();
                        HashMap<String, Integer> descriptorInterCount = new HashMap<>();

                        //change the date
                        PreparedStatement interactionsBeforeTimestampStatement = MySQLConnection.prepareStatement("SELECT * FROM interactions WHERE created <= ?");
                        Timestamp interactionTimestamp = new java.sql.Timestamp(newInteractions.getTimestamp(4).getTime());
                        interactionsBeforeTimestampStatement.setTimestamp(1, interactionTimestamp);

                        ResultSet interactionsBeforeInteractionTimestamp = interactionsBeforeTimestampStatement.executeQuery();

                        while (interactionsBeforeInteractionTimestamp.next()) {
                            String interactionType = interactionsBeforeInteractionTimestamp.getString("interactionType");
                            String performedBy = interactionsBeforeInteractionTimestamp.getString("performedBy");
                            String executedOver = interactionsBeforeInteractionTimestamp.getString("executedOver");

                            if (
                                    interactionType.equals("accept_smart_descriptor_in_metadata_editor") ||
                                            interactionType.equals("accept_descriptor_from_manual_list") ||
                                            interactionType.equals("accept_favorite_descriptor_in_metadata_editor") ||
                                            interactionType.equals("favorite_descriptor_from_quick_list_for_project")
                                    ) {
                                if (!users.containsKey(performedBy)) {
                                    users.put(performedBy, (long) users.keySet().size());
                                    //System.out.println("Adding user : " + nextLine[PERFORMED_BY]);
                                }

                                if ((descriptors.get(executedOver) == null)) {
                                    descriptors.put(executedOver, (long) descriptors.size());
                                    //System.out.println("Adding descriptor : " + nextLine[EXECUTED_OVER]);
                                }

                                if (!frequencies.containsKey(performedBy)) {
                                    frequencies.put(performedBy, new HashMap<String, Integer>());
                                }

                                HashMap<String, Integer> currentFrequencies = frequencies.get(performedBy);

                                if (!currentFrequencies.containsKey(executedOver)) {
                                    currentFrequencies.put(executedOver, 1);
                                } else {
                                    Integer currentUserDescriptorFrequency = currentFrequencies.get(executedOver);
                                    currentFrequencies.put(executedOver, currentUserDescriptorFrequency + 1);
                                }

                                if (userInterCount.containsKey(performedBy)) {
                                    userInterCount.put(performedBy, userInterCount.get(performedBy) + 1);
                                } else {
                                    userInterCount.put(performedBy, 1);
                                }

                                if (descriptorInterCount.containsKey(executedOver)) {
                                    descriptorInterCount.put(executedOver, descriptorInterCount.get(executedOver) + 1);
                                } else {
                                    descriptorInterCount.put(executedOver, 1);
                                }
                            }
                        }

                        Vector<String> descriptorsArray = new Vector<>(descriptors.keySet());

                        RecommendationSettings currentBestSettings = RecommendationSettings.getCurrentBest();

                        int bestRatingMode = currentBestSettings.getRatingSetting();
                        int bestNormalizationType = currentBestSettings.getNormalizationSetting();
                        double bestError = currentBestSettings.getError();

                        for (int n = 0; n < NORMALIZATION_TYPES.length; n++) {
                            int normalizationType = NORMALIZATION_TYPES[n];

                            for (int r = 0; r < RATING_MODES.length; r++) {
                                int ratingMode = RATING_MODES[r];
                                int actualNormalizationType = normalizationType;

                                FastByIDMap<PreferenceArray> userIdMap = new FastByIDMap<>();

                                for (String user : frequencies.keySet()) {
                                    HashMap<String, Integer> userFreqs = frequencies.get(user);
                                    GenericUserPreferenceArray userPreferences = new GenericUserPreferenceArray(descriptors.size());


                                    for (int j = 0; j < descriptorsArray.size(); j++) {
                                        String descriptor = descriptorsArray.get(j);

                                        if (userFreqs.containsKey(descriptor)) {
                                            Integer preference = userFreqs.get(descriptor);

                                            long userId = users.get(user);

                                            long descriptorId = descriptors.get(descriptor);
                                            if (descriptors.get(descriptor) == null) {
                                                userPreferences.set(j, new GenericPreference(userId, descriptorId, 0));
                                            } else {
                                                switch (ratingMode) {
                                                    case BOOLEAN_RATING_MODE: {
                                                        userPreferences.set(j, new BooleanPreference(userId, descriptorId));
                                                        break;
                                                    }
                                                    case ABSOLUTE_FREQUENCIES_RATING_MODE: {
                                                        Mean meanObject = new Mean();
                                                        double[] values = Utils.getDoubleArrayFromIntegerSet(userFreqs.values());
                                                        double mean = meanObject.evaluate(values);

                                                        StandardDeviation stdDevObject = new StandardDeviation();
                                                        double stdDev = stdDevObject.evaluate(values);

                                                        if(stdDev <=0)
                                                        {
                                                            actualNormalizationType = AVERAGE_NORMALIZATION;
                                                        }
                                                        double normalizedPreference = RecommendationHelper.normalize(actualNormalizationType, preference, mean, stdDev);
                                                        userPreferences.set(j, new GenericPreference(userId, descriptorId, (float) normalizedPreference));
                                                        break;
                                                    }
                                                    case RELATIVE_FREQUENCIES_RATING_MODE: {
                                                        int sumOfFrequencies = userInterCount.get(user);
                                                        float relativeFrequency = preference.floatValue() / sumOfFrequencies;

                                                        userPreferences.set(j, new GenericPreference(userId, descriptorId, relativeFrequency));
                                                        break;
                                                    }

                                                    case TF_IDF_RATING_MODE: {
                                                        float frequency = preference.floatValue();
                                                        float inverseDescriptorFrequency = 1.0f / descriptorInterCount.get(descriptor);
                                                        float rating = frequency * inverseDescriptorFrequency;

                                                        Mean meanObject = new Mean();
                                                        double[] values = Utils.getDoubleArrayFromIntegerSet(userFreqs.values());
                                                        double mean = meanObject.evaluate(values);

                                                        StandardDeviation stdDevObject = new StandardDeviation();
                                                        double stdDev = stdDevObject.evaluate(values);

                                                        if(stdDev <=0)
                                                        {
                                                            actualNormalizationType = AVERAGE_NORMALIZATION;
                                                        }

                                                        double normalizedRating = RecommendationHelper.normalize(actualNormalizationType, rating, mean, stdDev);
                                                        userPreferences.set(j, new GenericPreference(userId, descriptorId, (float) normalizedRating));

                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    long userId = users.get(user);
                                    userIdMap.put(userId, userPreferences);
                                }

                                /**
                                 * Mahout magic
                                 */

                                DataModel model = new GenericDataModel(userIdMap);

                                class MyRecommenderBuilder implements RecommenderBuilder {
                                    public Recommender buildRecommender(DataModel model) {
                                        UserSimilarity similarity = new LogLikelihoodSimilarity(model);

                                        UserNeighborhood neighborhood =
                                                new ThresholdUserNeighborhood(0.3, similarity, model);

                                        UserBasedRecommender recommender = new GenericUserBasedRecommender(
                                                model,
                                                neighborhood,
                                                similarity
                                        );

                                        return recommender;
                                    }
                                }

                                MyRecommenderBuilder builder = new MyRecommenderBuilder();

                                for (int evalRun = 0; evalRun < NUMBER_OF_EVALUATION_RUNS; evalRun++) {
                                    RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
                                    double result = evaluator.evaluate(builder, null, model, 0.9, 1.0);
                                    evaluationScores[evalRun] = result;
                                }

                                Mean mean = new Mean();
                                double meanError = mean.evaluate(evaluationScores);

                                if (meanError < bestError) {
                                    bestError = meanError;
                                    bestNormalizationType = normalizationType;
                                    bestRatingMode = ratingMode;

                                    System.out.println("New best error: " + meanError + " <<- " +
                                            ratingModesDescriptions.get(ratingMode) + " with normalization type " +
                                            normalizationModesDescriptions.get(bestNormalizationType) + " and rating mode " +
                                            ratingModesDescriptions.get(bestRatingMode));
                                }
                            }
                        }

                        RecommendationSettings bestSettings = new RecommendationSettings(
                                bestError,
                                bestNormalizationType,
                                bestRatingMode,
                                NUMBER_OF_EVALUATION_RUNS,
                                interactionTimestamp
                        );

                        bestSettings.saveToDatabase();
                        currentInteractionIndex++;
                    }
                }

                sleep(TIME_BETWEEN_UPDATES);

            } catch (SQLException e) {
                System.err.println("Unable to get last best settings for recommendation");
                e.printStackTrace();
            } catch (MathException e) {
                System.err.println("MATH EXCEPTION");
                e.printStackTrace();
            } catch (TasteException e) {
                System.err.println("TASTE EXCEPTION");
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("Unable to sleep during monitoring");
                e.printStackTrace();
            }
        }

        System.out.println("Recommendation tuner is not needed or is shutting down. See you later.");
    }
}
