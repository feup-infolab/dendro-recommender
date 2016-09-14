package helpers;

import com.google.gson.JsonObject;
import com.typesafe.config.ConfigException;
import connections.MySQLConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import play.Play;
import weka.core.Attribute;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Created by Paula on 5/23/2015.
 */
public class DendroTable {
    /** features from the database **/
    public static final String FREQUENTLY_USED_OVERALL = "frequently_used_overall";
    public static final String FREQUENTLY_USED_IN_PROJECT = "frequently_used_in_project";
    public static final String FREQUENTLY_USED_BY_USER = "frequently_used_by_user";
    public static final String FREQUENTLY_USED_BY_USER_IN_PROJECT = "frequently_used_by_user_in_project";
    public static final String USER_FAVORITE = "user_favorite";
    public static final String PROJECT_FAVORITE = "project_favorite";
    public static final String FAVORITE_OVERALL = "favorite_overall";
    public static final String FREQUENTLY_SELECTED_PROJECT = "frequently_selected_project";
    public static final String FREQUENTLY_SELECTED_OVERALL = "frequently_selected_overall";
    public static final String HIDDEN_FOR_USER = "hidden_for_user";
    public static final String HIDDEN_FOR_PROJECT = "hidden_for_project";

    public ArrayList<String> interactionBasedFeatures = null;

    public ArrayList<String> descriptorsList;
    public HashSet<String> ontologies;
    public ArrayList<String> features;
    public HashMap<String, HashMap<String, Double>> trainingTable;

    private WeightAccumulator accumulator;
    public HashMap<String, HashMap<String, Double>> hiddenTable;
    HashMap<String, Double> maximumFeatureDescriptorRow = new HashMap<>();

    public final static String INTERACTIONS_TABLE = Play.application().configuration().getString("persistence.mysql.interactions_table");

    public DendroTable(String queryFileLocation, String user, String project, HashSet<String> descriptorsToExclude, HashSet<String> allowedOntologies) throws IOException, SQLException, Exception {

        trainingTable = new HashMap<String, HashMap<String, Double>>();
        hiddenTable = new HashMap<>();

        //declare ontologies
        prepareOntologies(allowedOntologies);

        accumulator = new WeightAccumulator(ontologies);

        //declare features
        prepareFeatures();

        //get descriptors from dendro
        fetchDescriptors();

        //mark every descriptor as belonging to its ontology
        bindOntologies();

        String query = MySQLConnection.loadQuery(queryFileLocation);

        // dispatch query
        MySQLConnection connection = new MySQLConnection();
        Statement statement = connection.prepareStatement(query);
        query = query.replace(":utilizadorzinho", user);
        query = query.replace(":projectozinho", project);
        query = query.replace(":interactions_table_name", INTERACTIONS_TABLE);

        //System.out.println("Query is : \n " + query);

        if (query.equals("NOPE")) {
            System.out.println("Failed to read file. Aborting");
            return;
        }

        ResultSet results = statement.executeQuery(query);

        // sum all the new values for the final normalization
        prepareWeights();

        //insert all the features that are gathered from the database query
        putFeaturesFromDatabase(results, descriptorsToExclude);

        normalizeTable();
        //removeHidden();


        //System.out.println("bo tem mel");
    }

    /************************************
     *          INITIALIZE
     ************************************/

    private void prepareOntologies(HashSet<String> allowedOntologies) {

        if(ontologies == null)
        {
            ontologies = new HashSet<String>();
        }
        else
        {
            ontologies.clear();
        }

        if(allowedOntologies == null || allowedOntologies.size() == 0)
        {
            ontologies.add("http://dendro.fe.up.pt/ontology/achem/");
            ontologies.add("http://dendro.fe.up.pt/ontology/BIODIV/0.1#");
            ontologies.add("http://dendro.fe.up.pt/ontology/BioOc#");
            ontologies.add("http://purl.org/dc/elements/1.1/");
            ontologies.add("http://dendro.fe.up.pt/ontology/dcb/");
            ontologies.add("http://purl.org/dc/terms/");
            ontologies.add("http://dendro.fe.up.pt/ontology/0.1/");
            ontologies.add("http://dendro.fe.up.pt/ontology/EcoGeorref/0.1#");
            ontologies.add("http://xmlns.com/foaf/0.1/");
            ontologies.add("http://dendro.fe.up.pt/ontology/gravimetry#");
            ontologies.add("http://dendro.fe.up.pt/ontology/hydrogen#");
            ontologies.add("http://dendro.fe.up.pt/ontology/research/");
            ontologies.add("http://dendro.fe.up.pt/ontology/trafficSim#");
            ontologies.add("http://dendro.fe.up.pt/ontology/cep/");
            ontologies.add("http://dendro.fe.up.pt/ontology/socialStudies#");
            ontologies.add("http://dendro.fe.up.pt/ontology/cfd#");
            ontologies.add("http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#");
            ontologies.add("http://www.semanticdesktop.org/ontologies/2007/01/19/nie#");
            ontologies.add("http://rdfs.org/ns/void#");
        }
        else
        {
            ontologies.addAll(allowedOntologies);
        }
    }

    private void prepareFeatures() {

        if(interactionBasedFeatures == null)
        {
            interactionBasedFeatures = new ArrayList<String>();
            interactionBasedFeatures.add(FREQUENTLY_USED_OVERALL);
            interactionBasedFeatures.add(FREQUENTLY_USED_IN_PROJECT);
            interactionBasedFeatures.add(FREQUENTLY_USED_BY_USER);
            interactionBasedFeatures.add(FREQUENTLY_USED_BY_USER_IN_PROJECT);
            interactionBasedFeatures.add(USER_FAVORITE);
            interactionBasedFeatures.add(PROJECT_FAVORITE);
            interactionBasedFeatures.add(FAVORITE_OVERALL);
            interactionBasedFeatures.add(FREQUENTLY_SELECTED_PROJECT);
            interactionBasedFeatures.add(FREQUENTLY_SELECTED_OVERALL);
        }

        if(features == null)
        {
            features = new ArrayList<String>();
            features.addAll(ontologies);
            features.addAll(interactionBasedFeatures);
        }
    }

    private void prepareWeights() throws Exception
    {
        /**Overalls are the least important**/
        accumulator.setValue(WeightAccumulator.OVERALL, FREQUENTLY_USED_OVERALL, 0.05);
        accumulator.setValue(WeightAccumulator.OVERALL,FREQUENTLY_SELECTED_OVERALL, 0.01);

        /** Still, favorites are "explicit" feedback by users, so lets put them a more important type of overall **/
        accumulator.setValue(WeightAccumulator.OVERALL_FAVORITE,FAVORITE_OVERALL);

        /**selected in project should be very weak... still, projects are more important than overall**/
        accumulator.setValue(WeightAccumulator.SELECTED_IN_PROJECT, FREQUENTLY_SELECTED_PROJECT);

        for(String ontology : ontologies)
        {
            accumulator.setValue(ontology, ontology);
        }

        /**Medium importance, used means filled in, so these are very valued still**/
        accumulator.setValue(WeightAccumulator.USED_BY_USER, FREQUENTLY_USED_BY_USER);
        accumulator.setValue(WeightAccumulator.USED_IN_PROJECT, FREQUENTLY_USED_IN_PROJECT);
        accumulator.setValue(WeightAccumulator.USED_BY_USER_IN_PROJECT, FREQUENTLY_USED_BY_USER_IN_PROJECT);

        /**MOST IMPORTANT, user "explicit" feedback! **/
        accumulator.setValue(WeightAccumulator.USER_FAVORITE, USER_FAVORITE);
        accumulator.setValue(WeightAccumulator.FAVORITED_IN_PROJECT, PROJECT_FAVORITE);

        /** Print Weights **/
        accumulator.printWeights();
    }

    public ArrayList<Attribute> getFeaturesAttributes()
    {
        ArrayList<Attribute> attributes = new ArrayList<>();

        for(int i = 0; i < features.size(); i++)
        {
            String feature = features.get(i);

            for(int j = 0 ; j < attributes.size(); j++)
            {
                if(attributes.get(j).name().equals(feature))
                {
                    System.err.println("DEU ASNEIRA: TENS ATRIBUTO " + feature + " DUPLICADO");
                }
            }

            attributes.add(new Attribute(feature));
        }

        //System.out.println("Attributes are " + attributes.toString());

        return attributes;
    }

    public Double getDescriptorFeatureValue(String descriptor, String feature)
    {
        if(trainingTable.containsKey(descriptor))
        {
            HashMap<String, Double> featureValues = trainingTable.get(descriptor);
            if(featureValues.containsKey(feature))
            {
                return featureValues.get(feature);
            }
            else
            {
                return 0.0;
            }
        }
        else
        {
            return 0.0;
        }
    }

    public void setDescriptorFeatureValue(String descriptor, String feature, Double newValue)
    {
        if(!trainingTable.containsKey(descriptor))
        {
            trainingTable.put(descriptor, new HashMap<>());
        }

        trainingTable.get(descriptor).put(feature, newValue);
    }

    public void printOntologies(){
        System.out.println("Registered ontologies: \n");
        for (String s : ontologies) {
            System.out.println(s);
        }
    }

    /************************************
     *       ONTOLOGIES FEATURES
     ************************************/

    protected void bindOntologies() {

        //in the future delete the parameter results and get Ontologies and descriptors from a more complete list

        for (String desc : descriptorsList) {
            for (String ontology : ontologies) {
                if (desc.startsWith(ontology))
                {
                    setDescriptorFeatureValue(desc, ontology, 1d);
                }
            }
        }
    }

    /************************************
     *       OTHER FEATURES
     ************************************/

    public void putFeaturesFromDatabase(ResultSet results, HashSet<String> descriptorsToExclude) throws SQLException {

        ArrayList<String> hiddenDescriptors = new ArrayList<String>();

        while (results.next()) {

            String descriptor = results.getString("executedOver"); //get descriptor id

            if(descriptor != null) { //verificar se isto faz sentido

                if(!descriptorsToExclude.contains(descriptor))
                {
                    //consultar hashtable to get all the descriptor instance
                    if(trainingTable.containsKey(descriptor)) {
                        HashMap<String, Double> descriptorFeatures = trainingTable.get(descriptor);

                        // update new_values and sum the new values to the total hashtable

                        if (descriptor.equals("http://purl.org/dc/terms/accrualPeriodicity")) {
                            System.out.print("accrual");
                        }
                        for(String feature : interactionBasedFeatures) {

                            double valueFeature;

                            if (feature.equals(USER_FAVORITE)) {
                                valueFeature = makeTheDifference(results,"user_favorites", "user_unfavorites");

                            } else if (feature.equals(PROJECT_FAVORITE)) {
                                valueFeature = makeTheDifference(results,"project_favorites", "project_unfavorites");

                            } else if (feature.equals(FAVORITE_OVERALL)) {
                                valueFeature = makeTheDifference(results, "favorites_overall", "unfavorites_overall");

                            } else if (feature.equals(HIDDEN_FOR_USER)) {
                                valueFeature = makeTheDifference(results, "hiddens_for_user", "unhiddens_for_user");
                                if(valueFeature > 0.0){
                                    hiddenDescriptors.add(descriptor);
                                }
                            } else if (feature.equals(HIDDEN_FOR_PROJECT)) {
                                valueFeature = makeTheDifference(results, "hiddens_for_project", "unhiddens_for_project");
                                if(valueFeature > 0.0){
                                    hiddenDescriptors.add(descriptor);
                                }
                            } else {
                                valueFeature = results.getDouble(feature);
                            }

                            descriptorFeatures.put(feature, valueFeature);
                        }

                        //put in the hashtable again
                        //trainingTable.put(descriptor, descriptorFeatures);
                    }
                }
                else
                {
                    System.out.println("Excluding descriptor " + descriptor + " because it is not to be considered building Dendro Table (already filled in or hidden...)");
                }
            }
        }

        for (String s : hiddenDescriptors) {
            hiddenTable.put(s, trainingTable.get(s));
            trainingTable.remove(s);
        }
    }

    /**
     * Returns the difference between two associated features (favorite - favorite for instance)
     * @param positive
     * @param negative
     * @return
     */
    private double makeTheDifference(ResultSet results, String positive, String negative) throws SQLException
    {
        return results.getDouble(positive) - results.getDouble(negative);
    }

    public void calculateMaximums()
    {
        for(String feature : features)
        {
            double featureMax = Double.MIN_VALUE;

            for(String descriptor : trainingTable.keySet())
            {
                double descriptorFeatureValue = getDescriptorFeatureValue(descriptor, feature);
                if (descriptorFeatureValue > featureMax)
                {
                    featureMax = descriptorFeatureValue;
                    maximumFeatureDescriptorRow.put(feature, descriptorFeatureValue);
                }
            }
        }
    }

    public void normalizeTable() {

        calculateMaximums();

        Iterator it = trainingTable.entrySet().iterator();

        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            String descriptor = (String) pair.getKey();

            HashMap<String, Double> features = new HashMap<String, Double>((HashMap<String, Double>) pair.getValue());

            HashMap<String, Double> normalizedFeatures = new HashMap<String, Double>();
            HashMap<String, Double> weightedFeatures = new HashMap<String, Double>();

            normalizedFeatures = normalizeFeaturesByMax(features);

            try{
                //System.out.println("Normalizing features of descriptor " + descriptor);
                weightedFeatures = applyWeights(normalizedFeatures);
                trainingTable.put(descriptor, weightedFeatures);
            }
            catch(Exception e)
            {
                System.err.println("Error applying weights to features " + e.getLocalizedMessage());
            }
        }

    }

    //function that calculates the weights : https://www.desmos.com/calculator/z1znitefcu
    //\left(b\left(x-a\right)\right)^3\ +y_2
    // a = 1
    // y2 = 1
    // b = 0.5 (range -10...10)

    /**
     * Calculates the parameter B that gives the curve that will pass through (0, y1) and (1, y2)
     * @param y1
     * @return
     */
    private double calculateB(double y1, double y2)
    {
        double b = Math.cbrt(y1 - y2) / -1.0;
        return b;
    }

    private HashMap<String, Double> applyWeights(HashMap<String, Double> descriptorFeatures) throws Exception
    {
        HashMap<String, Double> weightedFeatures = new HashMap<>();

		//System.out.println("Category,Category Under,Feature,Scoring Formula");

        for(String feature : descriptorFeatures.keySet())
        {
            Double featureValue = descriptorFeatures.get(feature);

            String category = accumulator.getFeatureCategory(feature);
            String categoryUnder = accumulator.getPreviousCategory(category);

            double y1;
            if(categoryUnder == null)
                y1 = 0.0;
            else
                y1 = accumulator.getCategoryWeight(categoryUnder);

            double y2 = accumulator.getFeatureWeight(feature);

            if(y1 < y2)
            {
                double x = featureValue;
                double weighted;

                if(x == 0)
                {
                    weighted = 0.0;
                }
                else
                {
                    double b = calculateB(y1, y2);
					
                    weighted = Math.pow((b * (x - 1)), 3.0) + y2;
                    if(Boolean.parseBoolean(Play.application().configuration().getString("debug.print_formulas")))
                    {
                        System.out.println(category+","+categoryUnder+","+feature + "," + b + " * (x-1)^3 + " + y2 );
                    }
                }

                //System.out.println("Weight for feature " + feature + " : " + weighted);
                weightedFeatures.put(feature, weighted);
            }
            else if (y1 == y2)
            {
                double weighted = featureValue * y1;
                weightedFeatures.put(feature, weighted);
            }
            else
            {
                throw new Exception ("Error trying to calculate weights for feature. Y1 is " + y1 + " and Y2 is " + y2 + " but Y1 should always be < Y2!");
            }
        }

        return weightedFeatures;
    }

    private HashMap<String, Double> normalizeFeaturesByMax(HashMap<String, Double> descriptorFeatures)
    {
        HashMap<String, Double> normalizedFeatures = new HashMap<>();

        Iterator it = descriptorFeatures.entrySet().iterator();

        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            String feature = (String) pair.getKey();
            //System.out.println(pair.getKey() + " = " + pair.getValue());

            Double maximum = maximumFeatureDescriptorRow.get(pair.getKey());
            Double featureValue = (Double) pair.getValue();

            Double normalizedFeature = 0.0;
            if(maximum != null && maximum != 0.0)
            {
                normalizedFeature = (featureValue / maximum);
            }

            normalizedFeatures.put(feature, normalizedFeature);
        }

        return normalizedFeatures;
    }

    private HashMap<String, Double> normalizeFeaturesByNorm(HashMap<String, Double> descriptorFeatures)
    {
        HashMap<String, Double> normalizedFeatures = new HashMap<>();

        Iterator featuresIt = descriptorFeatures.entrySet().iterator();

        double norm = 0.0;
        while (featuresIt.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry) featuresIt.next();
            norm = norm + Math.pow((Double) pair.getValue(), 2.0);
        }

        norm = Math.sqrt(norm);

        for(String feature : descriptorFeatures.keySet())
        {
            Double value = descriptorFeatures.get(feature);
            Double normalizedValue = value / norm;
            normalizedFeatures.put(feature, normalizedValue);
        }

        return normalizedFeatures;
    }

    /************************************
     *      HANDLE HIDDEN DESCRIPTORS
     ************************************/

    public void removeHidden() {
        Iterator it = trainingTable.keySet().iterator();
        while (it.hasNext()) {
            String descriptor = (String) it.next();
            Double hiddenForUser = getDescriptorFeatureValue(descriptor, HIDDEN_FOR_USER);
            Double hiddenForProject = getDescriptorFeatureValue(descriptor, HIDDEN_FOR_PROJECT);

            if(hiddenForUser != 0.0 || hiddenForProject != 0.0)
            {
                trainingTable.remove(descriptor);
            }
        }
    }

    /**
     * Get descriptors list
     */

    public ArrayList<String> fetchDescriptors() throws IOException, ParseException {
        return fetchDescriptors(false);
    }

    public ArrayList<String> fetchDescriptors(boolean forceReload) throws IOException, ParseException {

        /*String allOntologiesEndpointUri = Play.application().configuration().getString("ontologies.all_ontologies_uri");
        URL obj = new URL(allOntologiesEndpointUri);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();*/

        if(descriptorsList == null)
        {
            //Entries will be added here
            descriptorsList = new ArrayList<>();

            String allOntologiesCacheFile = Play.application().configuration().getString("ontologies.all_ontologies_cache_file");

            Path inputPath = Paths.get(allOntologiesCacheFile);
            Path fullPath = inputPath.toAbsolutePath();

            JSONParser parser = new JSONParser();

            JSONArray ontologies = (JSONArray) parser.parse(new FileReader(new File(fullPath.toString())));

            for (int i = 0; i < ontologies.size(); ++i)
            {
                //Ontology level
                JSONObject ontologyObject = (JSONObject) ontologies.get(i);
                String ontologyUri = (String) ontologyObject.get("uri");

                JSONObject elementsObject = (JSONObject) ontologyObject.get("elements");

                //System.out.println("\nOntology " + ontologyUri.toString() + " : ");

                //do not add private ontologies to the recommendations system
                if (this.ontologies.contains(ontologyUri))
                {
                    Map<String, Object> retMap = toMap(elementsObject);
                    for (String entry : retMap.keySet())
                    {
                        descriptorsList.add(ontologyObject.get("uri") + entry);
                        //System.out.println(ontologyObject.get("uri") + entry);
                    }
                }
            }

            System.out.println("Got " + descriptorsList.size() + " descriptors from the Dendro instance.");

            return descriptorsList;
        }
        else
        {
            return descriptorsList;
        }
    }

    /**
     * As in http://stackoverflow.com/questions/21720759/convert-a-json-string-to-a-hashmap
     * @param object
     * @return
     */
    public static Map<String, Object> toMap(JSONObject object){
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keySet().iterator();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }


    public HashMap<String, Double> getDescriptorFeatures(String descriptor)
    {
        return trainingTable.get(descriptor);
    }

    /**
     * Iterates through the descriptors and adds them if
     * they are either an user's or project's favorite
     * @return an array of favorite descriptors
     */
    public ArrayList<String> getUserFavoritesDescriptors()
    {
        ArrayList<String> descriptors = new ArrayList<String>();
        Iterator it = trainingTable.keySet().iterator();
        while (it.hasNext()) {
            String descriptor = (String) it.next();

            Double userFavoriteValue = getDescriptorFeatureValue(descriptor, USER_FAVORITE);
            Double projectFavoriteValue = getDescriptorFeatureValue(descriptor, PROJECT_FAVORITE);

            if (userFavoriteValue > 0.0 || projectFavoriteValue > 0.0) {
                descriptors.add(descriptor);
            }
        }
        return descriptors;
    }

    public ArrayList<String> getHiddenDescriptors() {

        ArrayList<String> hiddenDescriptors = new ArrayList<>();
        hiddenDescriptors.addAll(hiddenTable.keySet());
        return hiddenDescriptors;
    }

    public HashMap<String, Object> getRecommendationTypes(String descriptor)
    {
        HashMap<String, Object> recommendationTypes = new HashMap<String, Object>();
        HashMap<String, Double> featuresValues = getDescriptorFeatures(descriptor);

        Iterator it = featuresValues.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            if((Double) pair.getValue() > 0.0){
                recommendationTypes.put((String) pair.getKey(),"true");
            }
        }

        return recommendationTypes;
    }
}
