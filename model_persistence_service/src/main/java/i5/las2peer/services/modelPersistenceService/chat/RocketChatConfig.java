package i5.las2peer.services.modelPersistenceService.chat;

import org.json.simple.JSONObject;

/**
 * RocketChat instance configuration.
 */
public class RocketChatConfig {

    /**
     * Public url of RocketChat instance.
     */
    private String url;

    /**
     * Auth token of bot account that is used to make API calls.
     */
    private String botAuthToken;

    /**
     * User ID of bot account that is used to make API calls.
     */
    private String botUserId;

    public RocketChatConfig(String url, String botAuthToken, String botUserId) {
        this.url = url;
        this.botAuthToken = botAuthToken;
        this.botUserId = botUserId;
    }

    public String getUrl() {
        return url;
    }

    public String getBotAuthToken() {
        return botAuthToken;
    }

    public String getBotUserId() {
        return botUserId;
    }
}
