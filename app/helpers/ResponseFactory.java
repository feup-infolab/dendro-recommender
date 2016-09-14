package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.Play;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by joaorocha on 26/08/14.
 */
public class ResponseFactory {

    private static JsonNode buildJsonMessage(String resultKey, String titleKey, String[] messageKeys)
    {
        String title = Play.application().configuration().getString(titleKey);
        String result = Play.application().configuration().getString(resultKey);

        HashSet<String> messages = new HashSet<>();
        for(int i = 0; i < messageKeys.length; i++)
        {
            messages.add(Play.application().configuration().getString(messageKeys[i]));
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("title", title);
        response.put("messages", messages);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(response);

        return json;
    }

    private static String buildHtmlMessage(String resultKey, String titleKey, String[] messageKeys)
    {
        String title = Play.application().configuration().getString(titleKey);

        String messages = "<ol>";
        for(int i = 0; i < messageKeys.length; i++)
        {
            messages += "<li>"+ Play.application().configuration().getString(messageKeys[i]) + "</li>";
        }
        messages += "</ol>";

        String result = Play.application().configuration().getString(resultKey);

        return "<html><h1>"+title+"</h1>" + "<h2>Result: "+result+"</h2>"+ messages + "</html>";
    }

    public static JsonNode buildJSONErrorResponse(String titleKey, String[] messageKeys)
    {

        return buildJsonMessage("messages.result.error", titleKey, messageKeys);
    }

    public static String buildHTMLErrorResponse(String titleKey, String[] messageKeys)
    {
        return buildHtmlMessage("messages.result.error", titleKey, messageKeys);
    }

    public static JsonNode buildJSONSuccessResponse(String titleKey, String[] messageKeys)
    {
        return buildJsonMessage("messages.result.ok", titleKey, messageKeys);
    }

    public static String buildHTMLSuccessResponse(String titleKey, String[] messageKeys)
    {
        return buildHtmlMessage("messages.result.ok", titleKey, messageKeys);
    }
}
