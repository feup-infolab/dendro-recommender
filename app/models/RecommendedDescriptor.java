package models;

import helpers.DendroTable;

import java.util.HashMap;

/**
 * Created by joaorocha on 27/05/15.
 */
public class RecommendedDescriptor implements Comparable {
    String descriptor;
    double score;

    public RecommendedDescriptor(String descriptor, double score) {
        this.descriptor = descriptor;
        this.score = score;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Object o)
    {
        RecommendedDescriptor otherDescriptor = (RecommendedDescriptor) o;
        double delta = this.score - otherDescriptor.score;

        if(delta < 0.0)
            return -1;
        else if(delta > 0.0)
            return 1;
        else
            return this.descriptor.compareTo(otherDescriptor.getDescriptor());
    }

    public HashMap<String, Object> serialize(DendroTable dt)
    {
        HashMap<String, Object> serializedRecommendation = new HashMap<>();
        serializedRecommendation.put("uri", getDescriptor());
        serializedRecommendation.put("score", getScore());
        serializedRecommendation.put("recommender", "scoring");


        HashMap<String, Object> recommendationTypes = dt.getRecommendationTypes(getDescriptor());

        serializedRecommendation.put("recommendation_types", recommendationTypes);

        HashMap<String, Object> features = new HashMap<>();

        for (String feature : dt.features) {
            features.put(feature, dt.getDescriptorFeatureValue(getDescriptor(), feature));
        }

        serializedRecommendation.put("features", features);

        return serializedRecommendation;
    }
}
