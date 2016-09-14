package recommenders;

import connections.MySQLConnection;
import helpers.DendroTable;
import helpers.Utils;
import models.RecommendedDescriptor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Created by ricardo on 13-06-2015.
 */
public class HiddenBasedRecommender {

    public static List<HashMap<String, Object>> recommend(DendroTable dt, String user, String project, int howMany) throws SQLException {

        //has to be in the conf folder so that it can be included in packaging for production...
        String query = MySQLConnection.loadQuery(Utils.getAbsFilePath("conf/files/queries/get_hidden_descriptors.sql"));

        // dispatch query
        MySQLConnection connection = new MySQLConnection();
        Statement statement = connection.prepareStatement(query);
        query = query.replace(":utilizadorzinho", user);
        query = query.replace(":projectozinho", project);
        query = query.replace(":interactions_table_name", DendroTable.INTERACTIONS_TABLE);

        //System.out.println("Query is : \n " + query);

        if (query.equals("NOPE")) {
            System.out.println("Failed to read file. Aborting");
            return null;
        }

        ResultSet results = statement.executeQuery(query);
        HashSet<String> hiddenDescriptors = new HashSet<>();

        PriorityQueue<RecommendedDescriptor> scoredDescriptors = new PriorityQueue<>(howMany, Collections.reverseOrder());


        HashSet<String> hiddenForUser = new HashSet<String>();
        HashSet<String> hiddenForProject = new HashSet<String>();

        while (results.next())
        {
            String descriptor = results.getString("descriptor"); //get descriptor url
            hiddenDescriptors.add(descriptor);

            RecommendedDescriptor scoredDescriptor = new RecommendedDescriptor(descriptor, 0.0);
            scoredDescriptors.add(scoredDescriptor);

            if(results.getInt("hidden_for_user") > 0)
            {
                hiddenForUser.add(descriptor);
            }

            if(results.getInt("hidden_for_project") > 0)
            {
                hiddenForProject.add(descriptor);
            }
        }

        List<HashMap<String, Object>> serializedRecommendations = new LinkedList<HashMap<String, Object>>();

        if(howMany > scoredDescriptors.size())
        {
            howMany = scoredDescriptors.size();
        }

        for(int i = 0; i < howMany ; i++)
        {
            RecommendedDescriptor recommendation = scoredDescriptors.poll();
            HashMap<String, Object> serializedRecommendation = recommendation.serialize(dt);

            HashMap<String, String> recommendationTypes = (HashMap<String, String>) serializedRecommendation.get("recommendation_types");

            if(hiddenForProject.contains(recommendation.getDescriptor()))
            {
                recommendationTypes.put("project_hidden", "true");
            }

            if(hiddenForUser.contains(recommendation.getDescriptor()))
            {
                recommendationTypes.put("user_hidden", "true");
            }

            serializedRecommendations.add(serializedRecommendation);
            System.out.println("Recommended HIDDEN Descriptor " + recommendation.getDescriptor() + " with score " + recommendation.getScore());
        }

        return serializedRecommendations;
    }
}
