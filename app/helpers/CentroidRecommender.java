package helpers;

import models.RecommendedDescriptor;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import weka.clusterers.SimpleKMeans;
import weka.core.*;
import weka.core.neighboursearch.LinearNNSearch;

import java.util.*;

/**
 * Created by joaorocha on 27/05/15.
 */
public class CentroidRecommender {

    public static String buildDescriptorHashLine(Instance instance)
    {
        double featuresLine[] = instance.toDoubleArray();
        String line = "";
        for(int i = 0; i < featuresLine.length; i++)
        {
            line = line + featuresLine[i];
            if(i < featuresLine.length -1 )
            {
                line = line + ",";
            }
        }

        //System.out.println("calculated line: " + line);
        return line;
    }

    /**
     * If the descriptor has any interaction feature != 0, it means that some interaction has been
     * performed over that descriptor.
     *
     * Determines if a descriptor should be included in the calculation of the centroid.
     * If there is no filter, the centroid will always be very close to the origin.
     * @param dt
     * @param descriptor
     * @return
     */
    public static boolean shouldBeConsideredForCentroidCalculation(DendroTable dt, String descriptor)
    {
        for(String interactionBasedFeature : dt.interactionBasedFeatures)
        {
            if(dt.getDescriptorFeatureValue(descriptor, interactionBasedFeature) != 0.0)
                return true;
        }

        return false;
    }

    public static List<HashMap<String, Object>>  recommendDescriptors (DendroTable dt, int howMany) throws Exception {

        ArrayList<Attribute> attributes = dt.getFeaturesAttributes();
        DualHashBidiMap<String, Vector<String>> uriMapper = new DualHashBidiMap<>();

        Instances instances = new Instances("dataset", attributes, dt.trainingTable.size());
        Instances instancesForCentroid = new Instances("dataset_for_centroid", attributes, dt.trainingTable.size());

        for (String descriptor : dt.trainingTable.keySet()) {
            Instance instance = new DenseInstance(dt.features.size());

            for (int i = 0; i < attributes.size(); i++)
            {
                Attribute feature = attributes.get(i);
                Double descriptorFeatureValue = dt.getDescriptorFeatureValue(descriptor, feature.name());
                instance.setValue(feature, descriptorFeatureValue);
            }

            //System.out.println("Descriptor " + descriptor + " has coordinates " + instance.toString());

            instances.add(instance);

            if (shouldBeConsideredForCentroidCalculation(dt, descriptor))
            {
                instancesForCentroid.add(instance);
            }

            String descriptorHashLine = buildDescriptorHashLine(instance);

            if (uriMapper.containsKey(descriptorHashLine))
            {
                Vector<String> descriptorsWithThisScore = uriMapper.get(descriptorHashLine);
                descriptorsWithThisScore.add(descriptor);
            } else
            {
                Vector<String> descriptorsWithThisScore = new Vector<>();
                descriptorsWithThisScore.add(descriptor);
                uriMapper.put(descriptorHashLine, descriptorsWithThisScore);
            }
        }

        SimpleKMeans kMeans = new SimpleKMeans();
        kMeans.setNumClusters(1);
        kMeans.buildClusterer(instances);

        //Chega aqui sem qualquer instância!
        SimpleKMeans kMeansCentroid = new SimpleKMeans();
        kMeansCentroid.setNumClusters(1);
        kMeansCentroid.buildClusterer(instancesForCentroid);

        Instances centroids = kMeansCentroid.getClusterCentroids();
        Instance centroid = centroids.get(0);

        System.out.println("Centroid : " + centroid.toString());

        LinearNNSearch knn = new LinearNNSearch(instances);
        Instances nearestInstances = knn.kNearestNeighbours(centroid, howMany);
        double[] distances = knn.getDistances();

        RecommendedDescriptor recommendations[] = new RecommendedDescriptor[nearestInstances.size()];

        List<HashMap<String, Object>> serializedRecommendations = new LinkedList<>();

        for (int i = 0; i < nearestInstances.size(); )
        {
            Instance nearestInstance = nearestInstances.get(i);
            Vector<String> descriptorUris = uriMapper.get(buildDescriptorHashLine(nearestInstance));

            int nTimesSameDescriptor = descriptorUris.size();

            for (int j = 0; j < nTimesSameDescriptor; j++)
            {
                String descriptorUri = descriptorUris.get(j);
                RecommendedDescriptor recommendation = new RecommendedDescriptor(descriptorUri, distances[i + j]);
                recommendations[i] = recommendation;

                //System.out.println("Recommended Descriptor " + (i+j) + " d: " + recommendation.getDescriptor() + " with score " + recommendation.getScore());

                HashMap<String, Object> serializedRecommendation = new HashMap<>();
                serializedRecommendation.put("uri", recommendation.getDescriptor());
                serializedRecommendation.put("score", recommendation.getScore());
                serializedRecommendation.put("recommender", "centroid_recommender");


                HashMap<String, Object> recommendationTypes = new HashMap<>();

                getRecommendationTypes(recommendation.getDescriptor(), dt, recommendationTypes);

                serializedRecommendation.put("recommendation_types", recommendationTypes);

                HashMap<String, Object> features = new HashMap<>();

                for (String feature : dt.features) {
                    features.put(feature, dt.getDescriptorFeatureValue(descriptorUri, feature));
                }

                serializedRecommendation.put("features", features);

                serializedRecommendations.add(serializedRecommendation);
            }

            i = i + nTimesSameDescriptor;
        }

        return serializedRecommendations;
    }

    private static void getRecommendationTypes(String descriptor, DendroTable dt, HashMap<String, Object> recommendationTypes)
    {
        HashMap<String, Double> featuresValues = dt.getDescriptorFeatures(descriptor);

        Iterator it = featuresValues.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            if((Double) pair.getValue() > 0.0){
                recommendationTypes.put((String) pair.getKey(),"true");
            }
        }
    }

}
