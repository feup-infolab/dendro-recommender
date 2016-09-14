package recommenders;

import helpers.DendroTable;
import models.RecommendedDescriptor;

import java.util.*;

/**
 * Created by ricardo on 13-06-2015.
 */
public class FavoriteBasedRecommender {

    public static List<HashMap<String, Object>> recommend(DendroTable dt, int howMany) {

        PriorityQueue<RecommendedDescriptor> scoredDescriptors = new PriorityQueue<>(howMany, Collections.reverseOrder());

        for (String descriptor : dt.trainingTable.keySet())
        {
            if(
                    (dt.getDescriptorFeatureValue(descriptor, DendroTable.PROJECT_FAVORITE) > 0.0)
                        ||
                    (dt.getDescriptorFeatureValue(descriptor, DendroTable.USER_FAVORITE) > 0.0)
              )
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


            System.out.println("Recommended FAVORITE Descriptor " + recommendation.getDescriptor() + " with score " + recommendation.getScore());
        }

        return serializedRecommendations;
    }
}
