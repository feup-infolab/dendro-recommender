package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import play.Play;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by joaorocha on 25/09/14.
 */
public class Utils
{
    public static void prettyPrintJSONString(String json)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
    }

    public static Set<JsonNode> serializeToJSONArray( ResultSet rs )
            throws SQLException
    {
        Set<JsonNode> result = new HashSet<>();
        ResultSetMetaData rsmd = rs.getMetaData();

        while(rs.next()) {
            int numColumns = rsmd.getColumnCount();

            Map<String, Object> obj = new HashMap<>();

            for (int i=1; i<numColumns+1; i++) {
                String column_name = rsmd.getColumnName(i);

                if(rsmd.getColumnType(i)==java.sql.Types.ARRAY){
                    obj.put(column_name, rs.getArray(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.BIGINT){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.BOOLEAN){
                    obj.put(column_name, rs.getBoolean(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.BLOB){
                    obj.put(column_name, rs.getBlob(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.DOUBLE){
                    obj.put(column_name, rs.getDouble(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.FLOAT){
                    obj.put(column_name, rs.getFloat(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.INTEGER){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.NVARCHAR){
                    obj.put(column_name, rs.getNString(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.VARCHAR){
                    obj.put(column_name, rs.getString(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.TINYINT){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.SMALLINT){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.DATE){
                    obj.put(column_name, rs.getDate(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.TIMESTAMP){
                    obj.put(column_name, rs.getTimestamp(column_name));
                }
                else{
                    obj.put(column_name, rs.getObject(column_name));
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.valueToTree(obj);

            result.add(json);
        }

        return result;
    }

    public static double[] getDoubleArrayFromIntegerSet(Collection<Integer> integerSet)
    {
        double values[] = new double[integerSet.size()];

        int i = 0;
        for (Integer value : integerSet)
        {
            values[i] = value;
        }

        return values;
    }

    public static String getPOSTParam(Map<String, String[]> values, String param)
    {
        if(values.get(param) != null && values.get(param).length == 1)
        {
            return values.get(param)[0];
        }
        else
        {
            return null;
        }
    }

    public static String getAbsFilePath(String relativeFilePath)
    {
        return Play.application().getFile(

                relativeFilePath

        ).getAbsolutePath();
    }
}
