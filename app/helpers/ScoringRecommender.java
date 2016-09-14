package helpers;

import models.RecommendedDescriptor;

import java.util.*;

/**
 * Created by joaorocha on 27/05/15.
 */
public class ScoringRecommender
{
    public static List<HashMap<String, Object>>  recommendDescriptors (DendroTable dt, int howMany) throws Exception {

        PriorityQueue<RecommendedDescriptor> scoredDescriptors = new PriorityQueue<>(howMany, Collections.reverseOrder());

        for (String descriptor : dt.trainingTable.keySet())
        {
            HashMap<String, Double> descriptorFeatures = dt.getDescriptorFeatures(descriptor);
            Double descriptorScore = 0.0;

            for(Double value : descriptorFeatures.values())
            {
                descriptorScore = descriptorScore + value;
            }

            RecommendedDescriptor scoredDescriptor = new RecommendedDescriptor(descriptor, descriptorScore);
            scoredDescriptors.add(scoredDescriptor);
        }

        List<HashMap<String, Object>> serializedRecommendations = new LinkedList<>();

        if(howMany > scoredDescriptors.size())
        {
            howMany = scoredDescriptors.size();
        }

        for(int i = 0; i < howMany ; i++)
        {
            RecommendedDescriptor recommendation = scoredDescriptors.poll();
            HashMap<String, Object> serializedRecommendation = recommendation.serialize(dt);
            serializedRecommendations.add(serializedRecommendation);


            //System.out.println("Recommended Descriptor " + recommendation.getDescriptor() + " with score " + recommendation.getScore());
        }

        return serializedRecommendations;
    }
}
