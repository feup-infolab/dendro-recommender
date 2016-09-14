package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.DendroTable;
import helpers.ResponseFactory;
import helpers.ScoringRecommender;
import helpers.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;
import recommenders.FavoriteBasedRecommender;
import recommenders.HiddenBasedRecommender;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Recommendation extends Controller {

    private final static String INTERACTIONS_TABLE = Play.application().configuration().getString("persistence.mysql.interactions_table");

    private static final String REC_ENTRY_ALL = "all";
    private static final String REC_ENTRY_FAVORITE = "favorites";
    private static final String REC_ENTRY_HIDDEN = "hidden";

    public static Result recommend() throws Exception {

        Map<String, String[]> v = request().body().asFormUrlEncoded();

        if  (
                (
                    Utils.getPOSTParam(v, "number_of_recommendations") != null
                    ||
                    (
                        Utils.getPOSTParam(v, "page") != null
                        &&
                        Utils.getPOSTParam(v, "page_size") != null
                    )
                ) &&
                        Utils.getPOSTParam(v, "user") != null &&
                        Utils.getPOSTParam(v, "current_resource") != null
            )
        {
            String userUri = Utils.getPOSTParam(v, "user");
            String currentResourceUri = Utils.getPOSTParam(v, "current_resource");
            String descriptorsFilter = Utils.getPOSTParam(v, "descriptor_filter");
            HashSet<String> allowedOntologies = new HashSet<>();

            if(v.containsKey("allowed_ontologies"))
            {
                JSONParser parser = new JSONParser();
                JSONArray allowedOntologiesJSON = (JSONArray) parser.parse(Utils.getPOSTParam(v, "allowed_ontologies"));

                for(int i = 0; i < allowedOntologiesJSON.size();i++)
                {
                    allowedOntologies.add((String) allowedOntologiesJSON.get(i));
                }
            }


            Path currentRelativePath = Paths.get("");
            String path = currentRelativePath.toAbsolutePath().toString();
            System.out.println("Current relative path is: " + path);

            String projectRegex = "http://[^/]+/project/[^/]+[/data]?";

            // Create a Pattern object
            Pattern r = Pattern.compile(projectRegex);

            // Now create matcher object.
            Matcher m = r.matcher(currentResourceUri);

            if(m.find()) {

                //if true, parse the list ahead and exclude its descriptors
                //[WARN] this is the list of descriptors that CANNOT be suggested
                boolean recommendAlreadyFilledIn = Boolean.parseBoolean(Utils.getPOSTParam(v, "recommend_already_filled_in"));
                HashSet<String> currentDescriptors = new HashSet<>();

                //TODO: parse json, extract descriptors and add each one of them to the currentDescriptors
                if (!recommendAlreadyFilledIn) {
                    System.err.println("ALREADY FILLED IN TRIGGERED");

                    JSONParser p = new JSONParser();

                    JSONObject currentDescriptorsObject = (JSONObject) p.parse(Utils.getPOSTParam(v, "current_metadata"));
                    JSONArray currentDescriptorsArray = (JSONArray) currentDescriptorsObject.get("descriptors");

                    for (int i = 0; i < currentDescriptorsArray.size() ; ++i)  {
                        JSONObject desc = (JSONObject) currentDescriptorsArray.get(i);
                        currentDescriptors.add((String) desc.get("uri"));
                    }
                }

                // PREPARE TABLE FOR RECOMMENDATION

                String projectUri = m.group();
                System.out.println("Project URI of resource " + currentResourceUri + " is " + projectUri + " .");

                //has to be in the conf folder so that it can be included in packaging for production...
                DendroTable d = new DendroTable(Utils.getAbsFilePath("conf/files/queries/fetch_interactions.sql"), userUri, projectUri, currentDescriptors, allowedOntologies);
                System.out.println("Features table was successfully built.");

                /*optional fields*/
                Integer numberOfRecommendations = null;
                try
                {
                    numberOfRecommendations = Integer.parseInt(Utils.getPOSTParam(v, "number_of_recommendations"));
                } catch(NumberFormatException e) {
                    System.out.println("[INFO] No Number of recommendations specified");
                }

                Integer pageNumber;
                Integer pageSize;

                try{
                    pageNumber = Integer.parseInt(Utils.getPOSTParam(v, "page"));
                } catch(NumberFormatException e) {
                    System.out.println("[WARN] Default page number (0) loaded");
                    pageNumber = 0;
                }

                try {
                    pageSize = Integer.parseInt(Utils.getPOSTParam(v, "page_size"));
                } catch(NumberFormatException e) {
                    System.out.println("[WARN] Default page size (10) loaded");
                    pageSize = 10;
                }

                if(numberOfRecommendations == null)
                {
                    numberOfRecommendations = pageSize * pageNumber + pageSize;
                    if(numberOfRecommendations >= d.fetchDescriptors().size() || numberOfRecommendations < 0)
                    {
                        numberOfRecommendations = d.fetchDescriptors().size();
                    }
                }

                System.out.println("Recommending " + numberOfRecommendations + " descriptors to user " + userUri + " which is in file / folder " + currentResourceUri);

                // READ FROM SQL FILE AND GET DATABASE


                List<HashMap<String, Object>> recommendations;

                if (descriptorsFilter == null) {
                    descriptorsFilter = REC_ENTRY_ALL;
                }

                int lowerBound = pageNumber * pageSize;

                switch (descriptorsFilter) {

                    case REC_ENTRY_FAVORITE:
                        recommendations
                                = FavoriteBasedRecommender.recommend(d, numberOfRecommendations);
                        break;

                    case REC_ENTRY_HIDDEN:
                        recommendations
                                = HiddenBasedRecommender.recommend(d, userUri, projectUri, numberOfRecommendations);
                        break;

                    default:
                        recommendations
                                = ScoringRecommender.recommendDescriptors(d, numberOfRecommendations);
                        break;
                }

                HashMap<String, Object> response = new HashMap<>();
                response.put("result", "ok");

                //List<HashMap<String, Object>> cfRecommendations = CFRecommender.recommendDescriptors(userUri, currentResourceUri, numberOfRecommendations);
                //List<HashMap<String, Object>> freqRecommendations = FrequenciesRecommender.recommendDescriptors(userUri, currentResourceUri, numberOfRecommendations);
                //List<HashMap<String, Object>> itemSimilarityRecommendations = ItemSimilarityRecommender.recommendDescriptors(userUri, currentResourceUri, numberOfRecommendations);

                //account for page and page number
                System.out.println("Returning page " + pageNumber + " with "
                        + pageSize + " recommendations, from "
                        + recommendations.size() + " recommendations");

                //I do have enough recommendations for a new page

                List<HashMap<String, Object>> slicedRecommendations = new LinkedList<>();

                int recommendationsSize = recommendations.size();
                if (lowerBound < recommendationsSize) {
                    int upperBound = lowerBound + pageSize;

                    if (upperBound > recommendations.size()) {
                        slicedRecommendations.addAll(recommendations.subList(lowerBound, recommendationsSize));
                    } else {
                        slicedRecommendations.addAll(recommendations.subList(lowerBound, upperBound));
                    }
                } else { //otherwise, there are not enough recommendations, return them all
                    slicedRecommendations = recommendations;
                }

                response.put("recommendations", slicedRecommendations);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.valueToTree(response);

                return ok(json);
            }
            else
            {
                return badRequest(ResponseFactory.buildHTMLErrorResponse(
                        "messages.error.invalid_api_request_title",
                        new String[]{"messages.error.missing_parameters"}
                )).as("text/html");
            }
        }
        else
        {
            return badRequest(ResponseFactory.buildHTMLErrorResponse(
                    "messages.error.invalid_api_request_title",
                    new String[]{"messages.error.missing_parameters"}
            )).as("text/html");
        }
    }
}
