package helpers;

import java.util.*;

/**
 * Created by joaorocha on 13/06/15.
 */
public class WeightAccumulator {

    private HashMap<String, ArrayList<String>> featuresByCategory = new HashMap<>();
    private HashMap<String, String> featureCategories = new HashMap<>();

    private HashMap<String, HashMap<String, Double>> featuresAndTheirWeightsByCategory = new HashMap<>();
    private HashMap<String, Double> weights = new HashMap<>();
    private HashSet<String> ontologies;

    private HashMap<String, Double> ontologyWeights = new HashMap<String, Double>();

    public static final String OVERALL = "overall";
    public static final String OVERALL_FAVORITE = "overall_favorite";
    public static final String SELECTED_IN_PROJECT = "selected_in_project";
    public static final String USED_IN_PROJECT = "used_in_project";
    public static final String FAVORITED_IN_PROJECT = "favorited_in_project";
    public static final String USED_BY_USER = "used_by_user";
    public static final String USED_BY_USER_IN_PROJECT= "used_by_user_in_project";
    public static final String USER_FAVORITE = "user_favorite";

    private ArrayList<String> allCategoriesOrderedByImportance;
    Map<String,String> categoryDescriptions;
    private ArrayList<Double> categoryMultipliers;

    public double getCategoryWeight(String category)
    {
        ArrayList<String> featuresUnder = featuresByCategory.get(category);
        ArrayList<Double> weightsOfFeaturesUnder = new ArrayList<Double>();

        int i = 0;
        for(String featureUnder : featuresUnder)
        {
            weightsOfFeaturesUnder.add(weights.get(featureUnder));
            i++;
        }

        double weight = Collections.max(weightsOfFeaturesUnder);

        return weight;
    }

    public double getWeightOfCategoryUnderTheCategoryOfThisFeature(String feature) throws Exception
    {
        if(featureCategories.containsKey(feature))
        {
            String category = featureCategories.get(feature);
            int categoryIndex = getCategoryIndex(category);

            if(categoryIndex == 0)
            {
                return 1.0;
            }
            else
            {
                String categoryUnder = getPreviousCategory(category);

                double weight = getCategoryWeight(categoryUnder);

                return weight;
            }
        }
        else if(weights.containsKey(feature))
        {
            return weights.get(feature);
        }
        else
        {
            throw new Exception("Unable to retrieve weight for feature " + feature);
        }
    }

    public double sumOfWeightsInCategory(String category)
    {
        double sum = 0.0;
        HashMap<String, Double> weightsForCategory = featuresAndTheirWeightsByCategory.get(category);
        for(Double weight : weightsForCategory.values())
        {
            sum += weight;
        }

        return sum;
    }

    public String getFeatureCategory(String feature)
    {
        return featureCategories.get(feature);
    }

    public String getPreviousCategory(String category) throws Exception
    {
        int previousCategoryIndex = getCategoryIndex(category) - 1;

        if(previousCategoryIndex < 0)
        {
            return null;
        }

        String previousCategory = allCategoriesOrderedByImportance.get(previousCategoryIndex);
        boolean previousCategoryIsAnOntology = ontologies.contains(previousCategory);
        boolean categoryIsAnOntology = ontologies.contains(category);

        while(
                previousCategoryIndex >= 0 && previousCategoryIsAnOntology && categoryIsAnOntology
            )
        {
            previousCategoryIndex--;
            previousCategory = allCategoriesOrderedByImportance.get(previousCategoryIndex);
            previousCategoryIsAnOntology = ontologies.contains(previousCategory);
        }

        return previousCategory;
    }

    public double sumOfWeightsInPreviousCategory(String category) throws Exception
    {
        String previousCategory = getPreviousCategory(category);
        return sumOfWeightsInCategory(previousCategory);
    }

    public void refreshTable() throws Exception
    {
        String firstCategory = allCategoriesOrderedByImportance.get(0);
        for(String featureInFirstCategory : featuresByCategory.get(firstCategory))
        {
            double value = featuresAndTheirWeightsByCategory.get(firstCategory).get(featureInFirstCategory);
            weights.put(featureInFirstCategory, value);
        }

        for(int i = 1; i < allCategoriesOrderedByImportance.size(); i++)
        {
            String category = allCategoriesOrderedByImportance.get(i);
            double multiplier = categoryMultipliers.get(i);

            double previousCategoryWeights = sumOfWeightsInPreviousCategory(category);
            double weightOfThisCategory = previousCategoryWeights * multiplier;

            ArrayList<String> features = featuresByCategory.get(category);

            for(String feature : features)
            {
                weights.put(feature, weightOfThisCategory);
                featuresAndTheirWeightsByCategory.get(category).put(feature,weightOfThisCategory);
            }
        }
    }

    public void setValue(String category, String featureKey) throws Exception
    {
        if(getCategoryIndex(category) >= 0)
        {
            featuresByCategory.get(category).add(featureKey);
            featureCategories.put(featureKey, category);
            refreshTable();
        }
        else
        {
            throw new Exception("There is no category  " + category + " when setting it as the category of " + featureKey);
        }
    }

    public void setValue(String category, String featureKey, Double value) throws Exception
    {
        if(getCategoryIndex(category) >= 0)
        {
            featuresByCategory.get(category).add(featureKey);
            featureCategories.put(featureKey, category);
            featuresAndTheirWeightsByCategory.get(category).put(featureKey, value);
        }
        else
        {
            throw new Exception("There is no category for supplied value " + category);
        }

        refreshTable();
    }

    public double getFeatureWeight(String feature) throws Exception
    {
        if(weights.containsKey(feature))
        {
            return weights.get(feature);
        }
        else
        {
            throw new Exception("There is no weight for feature " + feature + ". Maybe you forgot to initialize the weights? Call refreshTable() before this. ");
        }
    }

    public int getCategoryIndex(String category)
    {
        int categoryIndex = -1;
        for(int i = 0; i < allCategoriesOrderedByImportance.size(); i++)
        {
            if(allCategoriesOrderedByImportance.get(i).equals(category))
            {
                categoryIndex = i;
                break;
            }
        }

        return categoryIndex;
    }

    public void printWeights()
    {
        //System.out.println("\n");
        //System.out.println("Feature category;Feature;Weight");

        for(int i = 0; i < allCategoriesOrderedByImportance.size(); i++)
        {
            String category = allCategoriesOrderedByImportance.get(i);

            String categoryDescription = categoryDescriptions.get(category);

            if(categoryDescription == null && ontologies.contains(category))
            {
                categoryDescription = "Ontology membership";
            }

            HashMap<String, Double> featuresAndWeights = featuresAndTheirWeightsByCategory.get(category);

            for(String feature : featuresAndWeights.keySet())
            {
                //System.out.println(categoryDescription + ";"+ feature+";"+featuresAndWeights.get(feature));
            }
        }
        //System.out.println("\n");
    }

    public WeightAccumulator(HashSet<String> ontologies)
    {
        this.ontologies = ontologies;

        categoryDescriptions = new HashMap<String, String>();
        categoryDescriptions.put(OVERALL, "Overall selections and usages");
        categoryDescriptions.put(OVERALL_FAVORITE, "Overall favoritings");
        categoryDescriptions.put(SELECTED_IN_PROJECT, "Selections in project");
        categoryDescriptions.put(USED_IN_PROJECT, "Usages in a project");
        categoryDescriptions.put(FAVORITED_IN_PROJECT, "Favoritings in a project");
        categoryDescriptions.put(USED_BY_USER, "Usages by the user");
        categoryDescriptions.put(USED_IN_PROJECT, "Usages in the project");
        categoryDescriptions.put(USED_BY_USER_IN_PROJECT, "Usages by the user in the project");
        categoryDescriptions.put(USER_FAVORITE, "Favoritings by the user");

        allCategoriesOrderedByImportance = new ArrayList<>();
        categoryMultipliers = new ArrayList<Double>();

        allCategoriesOrderedByImportance.add(OVERALL);
        categoryMultipliers.add(1.0);

        allCategoriesOrderedByImportance.add(OVERALL_FAVORITE);
        categoryMultipliers.add(2.02);

        allCategoriesOrderedByImportance.add(SELECTED_IN_PROJECT);
        categoryMultipliers.add(2.02);


        Iterator<String> ontologiesIt = ontologies.iterator();

        while(ontologiesIt.hasNext())
        {
            String ontology = ontologiesIt.next();
            allCategoriesOrderedByImportance.add(ontology);
            categoryMultipliers.add(2.02);
        }

        allCategoriesOrderedByImportance.add(USED_IN_PROJECT);
        categoryMultipliers.add(2.02);

        allCategoriesOrderedByImportance.add(FAVORITED_IN_PROJECT);
        categoryMultipliers.add(2.02);

        allCategoriesOrderedByImportance.add(USED_BY_USER);
        categoryMultipliers.add(2.02);

        allCategoriesOrderedByImportance.add(USED_BY_USER_IN_PROJECT);
        categoryMultipliers.add(2.02);

        allCategoriesOrderedByImportance.add(USER_FAVORITE);
        categoryMultipliers.add(2.02);

        for(int i = 0; i < allCategoriesOrderedByImportance.size(); i++)
        {
            featuresByCategory.put(allCategoriesOrderedByImportance.get(i), new ArrayList<>());
            featuresAndTheirWeightsByCategory.put(allCategoriesOrderedByImportance.get(i), new HashMap<>());
        }
    }
}
