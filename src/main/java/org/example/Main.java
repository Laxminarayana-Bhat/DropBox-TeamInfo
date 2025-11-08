package org.example;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar DropBox-TeamInfo-1.0-SNAPSHOT.jar <CLIENT_ID> <CLIENT_SECRET> [port]");
            System.exit(1);
        }

        String clientId = args[0].trim();
        String clientSecret = args[1].trim();
        int port = 45678;//This one must be registered
        if (args.length >= 3) {
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
            }
        }

        String redirectUri = "http://localhost:" + port + "/callback";
        String scopes = "team_info.read team_data.member";

        OAuthCallbackServer server = null;
        try {
            server = new OAuthCallbackServer(port);
            server.start();

            String authUrl = DropBoxClient.buildAuthUrl(clientId, redirectUri, scopes);
            System.out.println("Open this URL in a browser to authorize the app:");
            System.out.println(authUrl);

            // Try open browser automatically
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(authUrl));
                }
            } catch (Exception e) {
                System.out.println("(Could not open browser automatically: " + e.getMessage() + ")");
            }

            System.out.println("Waiting for authorization (5 minutes timeout)...");
            String code = server.waitForCode(5, TimeUnit.MINUTES);
            System.out.println("Got authorization code.");

            DropBoxClient client = new DropBoxClient(clientId, clientSecret, redirectUri);
            String tokenResponse = client.exchangeCodeForToken(code);
            System.out.println("Token response:\n" + tokenResponse);

            String accessToken = client.extractAccessToken(tokenResponse);
            if (accessToken == null) {
                System.err.println("Failed to extract access_token from token response.");
                System.exit(2);
            }

            String teamInfo = client.callTeamGetInfo(accessToken);
            System.out.println("\n=== team/get_info response ===");
            System.out.println(teamInfo);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            if (server != null) server.stop();
        }
    }
}
