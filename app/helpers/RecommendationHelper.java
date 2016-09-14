package helpers;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

/**
 * Created by joaorocha on 17/05/15.
 */
public class RecommendationHelper {

   public static double normalize(
            int normalizationType,
            double absoluteRating,
            double mean,
            double stdDev)
            throws MathException
    {
        if(normalizationType==RecommendationTuner.AVERAGE_NORMALIZATION)
        {
            return absoluteRating - mean;
        }
        else if (normalizationType==RecommendationTuner.GAUSSIAN_NORMALIZATION)
        {
            NormalDistribution normal = new NormalDistributionImpl(mean, stdDev);

            return normal.density(absoluteRating);
        }
        else if (normalizationType==RecommendationTuner.DECOUPLING_NORMALIZATION)
        {
            NormalDistribution normal = new NormalDistributionImpl(mean, stdDev);

            double pRatingLessOrEqualThanR = normal.cumulativeProbability(absoluteRating);
            double pRatingEqualsR = normal.density(absoluteRating);

            return pRatingLessOrEqualThanR - pRatingEqualsR / 2.0;
        }
        else if (normalizationType==RecommendationTuner.NO_NORMALIZATION)
        {
            return absoluteRating;
        }
        else
        {
            System.err.println("Normalization mode " + normalizationType + " is unknown!");
            return -1;
        }
    }
}
