package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import connections.MySQLConnection;
import helpers.ResponseFactory;
import helpers.Utils;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Interactions extends Controller {

    private final static String INTERACTIONS_TABLE = Play.application().configuration().getString("persistence.mysql.interactions_table");

    public static Result latest(Integer howMany) throws SQLException {

        if(howMany < 0) {
            return all();
        }
        else
        {
            //Serialization of results
            if(request().accepts("application/json") && !request().accepts("text/html"))
            {
                String sql =
                        "SELECT * " +
                                "FROM "+INTERACTIONS_TABLE+" " +
                                "ORDER BY created DESC " +
                                "LIMIT ?";

                PreparedStatement s = MySQLConnection.prepareStatement(sql);
                s.setInt(1, howMany);
                ResultSet latestInteractions = s.executeQuery();

                Set<JsonNode> serializedInteractions = Utils.serializeToJSONArray(latestInteractions);

                Map<String, Object> response = new HashMap<>();
                response.put("result", "ok");
                response.put("interactions", serializedInteractions);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.valueToTree(response);

                //System.out.println("Resultado: " + json.toString());
                return ok(json);
            }
            else
            {
                return badRequest(ResponseFactory.buildHTMLErrorResponse(
                        "messages.error.invalid_api_request_title",
                        new String[]{"messages.error.invalid_accept_header.message"}
                )).as("text/html");
            }
        }
    };

    public static Result all() throws SQLException {

        //Serialization of results
        if(request().accepts("application/json") && !request().accepts("text/html"))
        {
            String sql =
                    "SELECT * " +
                    "FROM " + INTERACTIONS_TABLE;

            PreparedStatement s = MySQLConnection.prepareStatement(sql);
            ResultSet latestInteractions = s.executeQuery();

            Set<JsonNode> serializedInteractions = Utils.serializeToJSONArray(latestInteractions);

            Map<String, Object> response = new HashMap<>();
            response.put("result", "ok");
            response.put("interactions", serializedInteractions);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.valueToTree(response);

            return ok(json);
        }
        else
        {
            return badRequest(ResponseFactory.buildHTMLErrorResponse(
                    "messages.error.invalid_api_request_title",
                    new String[]{"messages.error.invalid_accept_header.message"}
            )).as("text/html");
        }
    }
}
