package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.ResponseFactory;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.HashMap;
import java.util.Map;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.home.index.render("Your new application is ready!!!!!!!!!!!!!!!!"));
    }

    public static Result about() {

        if (request().accepts("application/json") && !request().accepts("text/html"))
        {
            Map<String, String> response = new HashMap<>();
            response.put("result", "ok");
            response.put("message", "Dendro Recommender Online");
            response.put("version", Play.application().configuration().getString("application.version"));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.valueToTree(response);
            return ok(json);
        }
        else
        {
            return badRequest(ResponseFactory.buildHTMLErrorResponse(
                    "messages.error.invalid_api_request_title",
                    new String[]{
                            "messages.error.invalid_accept_header.message",
                    }
            )).as("text/html");
        }
    }
}
