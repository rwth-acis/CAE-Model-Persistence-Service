package i5.las2peer.services.modelPersistenceService.chat;

import com.google.common.io.Resources;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RocketChatHelper {

    public String createIntegration(RocketChatConfig config, String channelName) {
        String scriptContent = "";
        try {
            InputStream in = getClass().getResourceAsStream("/rocketchat_integration.js");
            scriptContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject body = new JSONObject();
        body.put("type", "webhook-incoming");
        body.put("username", "CAE");
        body.put("name", "CAE");
        body.put("enabled", true);
        body.put("channel", "#" + channelName);
        body.put("scriptEnabled", true);
        body.put("alias", "GitHub");
        body.put("avatar", "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png");
        body.put("script", scriptContent);

        HttpResponse<String> response = Unirest.post(config.getUrl() + "/api/v1/integrations.create")
                .header("X-Auth-Token", config.getBotAuthToken())
                .header("X-User-Id", config.getBotUserId())
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .asString();

        String webhookUrl = null;
        if(response.isSuccess()) {
            JSONObject responseBody = (JSONObject) JSONValue.parse(response.getBody());
            JSONObject integration = (JSONObject) responseBody.get("integration");
            String id = (String) integration.get("_id");
            String token = (String) integration.get("token");
            webhookUrl = config.getUrl() + "/hooks/" + id + "/" + token;
        }
        return webhookUrl;
    }

    private static String listIntegrations(RocketChatConfig config) {
        HttpResponse<String> response = Unirest.get(config.getUrl() + "/api/v1/integrations.list")
                .header("X-Auth-Token", config.getBotAuthToken())
                .header("X-User-Id", config.getBotUserId())
                .header("Content-Type", "application/json")
                .asString();
        if(response.isSuccess()) {
            return response.getBody();
        }
        return null;
    }

    public static String getIntegrationWebhookUrl(RocketChatConfig config, String channelId) {
        JSONObject result = (JSONObject) JSONValue.parse(listIntegrations(config));
        JSONArray integrations = (JSONArray) result.get("integrations");
        for(Object o : integrations) {
            JSONObject integration = (JSONObject) o;

            String username = (String) integration.get("username");
            if(!username.equals("CAE")) continue;

            String alias = (String) integration.get("alias");
            if(!alias.equals("GitHub")) continue;

            String type = (String) integration.get("type");
            if(!type.equals("webhook-incoming")) continue;

            JSONArray channel = (JSONArray) integration.get("channel");
            for(Object c : channel) {
                String channelName = (String) c;
                if(channelName.equals("#" + channelId)) {
                    String id = (String) integration.get("_id");
                    String token = (String) integration.get("token");
                    return config.getUrl() + "/hooks/" + id + "/" + token;
                }
            }
        }
        return null;
    }

}
