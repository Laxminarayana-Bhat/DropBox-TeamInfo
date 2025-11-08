package org.example;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class DropBoxClient {

    private static final String AUTH_BASE = "https://www.dropbox.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.dropboxapi.com/oauth2/token";
    private static final String TEAM_GET_INFO = "https://api.dropboxapi.com/2/team/get_info";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    //Constructor
    public DropBoxClient(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    //Function to encode and
    public static String buildAuthUrl(String clientId, String redirectUri, String scopes) {
        String url = AUTH_BASE
                + "?response_type=code"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        if (scopes != null && !scopes.isEmpty()) {
            url += "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8);
        }
        // token_access_type not requested here (no refresh token)
        return url;
    }


    public String exchangeCodeForToken(String code) throws Exception {
        String form = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Token endpoint error (" + resp.statusCode() + "): " + resp.body());
        }
        return resp.body();
    }

    public String extractAccessToken(String tokenResponseBody) {
        // simple extractor: find "access_token":"..."; works for Dropbox JSON response
        String key = "\"access_token\"";
        int i = tokenResponseBody.indexOf(key);
        if (i < 0) return null;
        int colon = tokenResponseBody.indexOf(':', i + key.length());
        if (colon < 0) return null;
        int firstQuote = tokenResponseBody.indexOf('"', colon);
        if (firstQuote < 0) return null;
        int secondQuote = tokenResponseBody.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return tokenResponseBody.substring(firstQuote + 1, secondQuote);
    }

    public String callTeamGetInfo(String accessToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TEAM_GET_INFO))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("null"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("API error (" + resp.statusCode() + "): " + resp.body());
        }
        return resp.body();
    }
}
