package models;

import connections.MySQLConnection;
import helpers.RecommendationTuner;

import java.sql.*;
import java.sql.Timestamp;

/**
 * Created by joaorocha on 17/05/15.
 */
public class RecommendationSettings {

    private double error;
    private int normalizationSetting;
    private int ratingSetting;
    private Timestamp evaluationTimestamp;
    private int evaluationRuns;
    private Timestamp limitingInteractionTimestamp;

    public double getError() {
        return error;
    }

    public int getNormalizationSetting() {
        return normalizationSetting;
    }

    public int getRatingSetting() {
        return ratingSetting;
    }

    public Timestamp getLimitingInteractionTimestamp() {
        return limitingInteractionTimestamp;
    }

    public RecommendationSettings(
            double error,
            int normalizationSetting,
            int ratingSetting,
            int evaluationRuns,
            Timestamp limitingInteractionTimestamp)
    {
        this.error = error;
        this.normalizationSetting = normalizationSetting;
        this.ratingSetting = ratingSetting;
        this.evaluationRuns = evaluationRuns;
        this.limitingInteractionTimestamp = new java.sql.Timestamp(limitingInteractionTimestamp.getTime());
    }

    public RecommendationSettings(
            double error,
            int normalizationSetting,
            int ratingSetting,
            int evaluationRuns,
            java.util.Date limitingInteractionTimestamp,
            Timestamp evaluationTimestamp)
    {
        this.error = error;
        this.normalizationSetting = normalizationSetting;
        this.ratingSetting = ratingSetting;
        this.evaluationRuns = evaluationRuns;
        this.limitingInteractionTimestamp = new java.sql.Timestamp(limitingInteractionTimestamp.getTime());
        this.evaluationTimestamp = new java.sql.Timestamp(evaluationTimestamp.getTime());
    }

    public void saveToDatabase() throws SQLException
    {
        String sql =
                    "INSERT INTO recommendation_settings " +
                    "(error, normalizationSetting, ratingSetting, evaluationRuns, evaluationTimestamp, limitingInteractionTimestamp) " +
                    "VALUES ( ? , ?, ?, ?, ?, ?)";

        PreparedStatement statement = MySQLConnection.prepareStatement(sql);
        statement.setDouble(1, error);
        statement.setInt(2, normalizationSetting);
        statement.setInt(3, ratingSetting);
        statement.setInt(4, RecommendationTuner.NUMBER_OF_EVALUATION_RUNS);
        statement.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
        statement.setTimestamp(6, limitingInteractionTimestamp);

        statement.executeUpdate();
    }

    public static RecommendationSettings getCurrentBest() throws SQLException
    {
        String sql =    "SELECT " +
                        "error, normalizationSetting, ratingSetting, evaluationRuns, evaluationTimestamp, limitingInteractionTimestamp " +
                        "FROM recommendation_settings " +
                        "ORDER BY limitingInteractionTimestamp " +
                        "DESC LIMIT 1";

        PreparedStatement statement = MySQLConnection.prepareStatement(sql);
        ResultSet results = statement.executeQuery();

        if(results.next())
        {
            return new RecommendationSettings(
                    results.getDouble(1),
                    results.getInt(2),
                    results.getInt(3),
                    results.getInt(4),
                    results.getTimestamp(5),
                    results.getTimestamp(6)
            );
        }
        else
        {
            System.err.println("Unable to retrieve any current best recommendation settings");
            return new RecommendationSettings(Double.MAX_VALUE, 0, 0, RecommendationTuner.NUMBER_OF_EVALUATION_RUNS, new Timestamp(0));
        }
    }
}
