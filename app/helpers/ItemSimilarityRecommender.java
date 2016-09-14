package helpers;

import connections.MySQLConnection;
import models.RecommendationSettings;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
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
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by joaorocha on 19/05/15.
 */
public class ItemSimilarityRecommender {

    public static List<HashMap<String, Object>> recommendDescriptors (String userUri, String projectUri, int howMany) throws SQLException, MathException, TasteException
    {
        //TODO Substituir pelo modelo item similarity

        String sql = "SELECT * FROM interactions";
        PreparedStatement statement = MySQLConnection.prepareStatement(sql);
        ResultSet results = statement.executeQuery();

        DualHashBidiMap<String, Long> users = new DualHashBidiMap<>();
        DualHashBidiMap<String, Long> descriptors = new DualHashBidiMap<>();

        HashMap<String, HashMap<String, HashMap<String, Integer>>> frequencies = new HashMap<>();

        HashMap<String, Integer> userInterCount = new HashMap<>();
        HashMap<String, Integer> descriptorInterCount = new HashMap<>();

        while (results.next()) {

            String interactionType = results.getString("interactionType");
            String performedBy = results.getString("performedBy");
            String executedOver = results.getString("executedOver");

            if(frequencies.get(interactionType) == null)
            {
                frequencies.put(interactionType, new HashMap<String, HashMap<String, Integer>>());
            }
            else
            {
                if(!users.containsKey(performedBy))
                {
                    users.put(performedBy, (long) users.keySet().size());
                    //System.out.println("Adding user : " + nextLine[PERFORMED_BY]);
                }

                if((descriptors.get(executedOver) == null))
                {
                    descriptors.put(executedOver, (long) descriptors.size());
                    //System.out.println("Adding descriptor : " + nextLine[EXECUTED_OVER]);
                }

                if(!frequencies.containsKey(performedBy))
                {
                    frequencies.get(interactionType).put(performedBy, new HashMap<String, Integer>());
                }

                HashMap<String, Integer> currentFrequencies = frequencies.get(interactionType).get(performedBy);

                if(!currentFrequencies.containsKey(executedOver))
                {
                    currentFrequencies.put(executedOver, 1);
                }
                else
                {
                    Integer currentUserDescriptorFrequency = currentFrequencies.get(executedOver);
                    currentFrequencies.put(executedOver, currentUserDescriptorFrequency + 1);
                }

                if(userInterCount.containsKey(performedBy))
                {
                    userInterCount.put(performedBy, userInterCount.get(performedBy) + 1);
                }
                else
                {
                    userInterCount.put(performedBy, 1);
                }

                if(descriptorInterCount.containsKey(executedOver))
                {
                    descriptorInterCount.put(executedOver, descriptorInterCount.get(executedOver) + 1);
                }
                else
                {
                    descriptorInterCount.put(executedOver, 1);
                }
            }
        }

        Vector<String> descriptorsArray = new Vector<>(descriptors.keySet());

        RecommendationSettings settings = RecommendationSettings.getCurrentBest();

        int bestRatingMode = RecommendationTuner.RELATIVE_FREQUENCIES_RATING_MODE;
        int bestNormalizationType = RecommendationTuner.GAUSSIAN_NORMALIZATION;

        if(RecommendationTuner.isActive())
        {
            bestRatingMode = settings.getRatingSetting();
            bestNormalizationType = settings.getNormalizationSetting();
        }

        FastByIDMap<PreferenceArray> userIdMap = new FastByIDMap<>();

        Set<String> allInteractionTypes = frequencies.keySet();

        for (String interactionType : allInteractionTypes)
        {
            HashMap<String, HashMap<String, Integer>> interactionTypeFrequencies = frequencies.get(interactionType);
            for (String user : interactionTypeFrequencies.keySet())
            {
                HashMap<String, Integer> userFreqs = interactionTypeFrequencies.get(user);
                GenericUserPreferenceArray userPreferences = new GenericUserPreferenceArray(descriptors.size());

                Mean meanObject = new Mean();
                double[] values = Utils.getDoubleArrayFromIntegerSet(userFreqs.values());
                double mean = meanObject.evaluate(values);

                StandardDeviation stdDevObject = new StandardDeviation();
                double stdDev = stdDevObject.evaluate(values);

                for(int j = 0; j < descriptorsArray.size(); j++)
                {
                    String descriptor = descriptorsArray.get(j);

                    if(userFreqs.containsKey(descriptor))
                    {
                        Integer preference = userFreqs.get(descriptor);

                        long userId = users.get(user);

                        long descriptorId = descriptors.get(descriptor);
                        if(descriptors.get(descriptor) == null)
                        {
                            userPreferences.set(j, new GenericPreference(userId, descriptorId, 0));
                        }
                        else
                        {
                            switch (bestRatingMode)
                            {
                                case RecommendationTuner.BOOLEAN_RATING_MODE:
                                {
                                    userPreferences.set(j, new BooleanPreference(userId, descriptorId));
                                    break;
                                }
                                case RecommendationTuner.ABSOLUTE_FREQUENCIES_RATING_MODE :
                                {
                                    double normalizedPreference = RecommendationHelper.normalize(bestNormalizationType, preference, mean, stdDev);
                                    userPreferences.set(j, new GenericPreference(userId, descriptorId, (float) normalizedPreference));
                                    break;
                                }
                                case RecommendationTuner.RELATIVE_FREQUENCIES_RATING_MODE :
                                {
                                    int sumOfFrequencies = userInterCount.get(user);
                                    float relativeFrequency = preference.floatValue() / sumOfFrequencies;

                                    userPreferences.set(j, new GenericPreference(userId, descriptorId, relativeFrequency));
                                    break;
                                }

                                case RecommendationTuner.TF_IDF_RATING_MODE :
                                {
                                    float frequency = preference.floatValue();
                                    float inverseDescriptorFrequency = 1.0f / descriptorInterCount.get(descriptor);
                                    float rating = frequency * inverseDescriptorFrequency;

                                    double normalizedRating = RecommendationHelper.normalize(bestNormalizationType, rating, mean, stdDev);
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
        }

        /**
         * Mahout magic
         */

        DataModel model = new GenericDataModel(userIdMap);

        class MyRecommenderBuilder implements RecommenderBuilder
        {
            public Recommender buildRecommender(DataModel model)
            {
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
        Recommender recommender = builder.buildRecommender(model);

        Long userId = users.get(userUri);

        if(userId == null)
        {
            System.out.println("User " + userUri + " has no interactions recorded.");
            return new LinkedList<>();
        }
        else
        {
            List<RecommendedItem> recommendations = recommender.recommend(userId, howMany);
            List<HashMap<String, Object>> serializedRecommendations = new LinkedList<>();

            for(RecommendedItem recommendation : recommendations)
            {
                HashMap<String, Object> serializedRecommendation = new HashMap<>();
                serializedRecommendation.put("uri", descriptors.getKey(recommendation.getItemID()));
                serializedRecommendation.put("score", recommendation.getValue());
                serializedRecommendation.put("recommendation_type", "item_similarity_recommender");


                serializedRecommendations.add(serializedRecommendation);
            }

            return serializedRecommendations;
        }
    }
}
