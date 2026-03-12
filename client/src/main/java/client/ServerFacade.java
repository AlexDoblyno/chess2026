package client;

import chess.ChessGame;
import com.google.gson.Gson;
import Models.*;
import exception.ResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

public class ServerFacade {

    private final String serverUrl;

    public ServerFacade(String url) {
        serverUrl = url;
    }

    public AuthTokenData registerUser(UserData userData) throws ResponseException {
        var path = "/user";
        return this.makeRequest("POST", path, userData, AuthTokenData.class, null);
    }

    public AuthTokenData loginUser(String username, String password) throws ResponseException {
        var path = "/session";
        LoginRequest loginRequest = new LoginRequest(username, password);
        return this.makeRequest("POST", path, loginRequest, AuthTokenData.class, null);
    }

    public void logoutUser(String authToken) throws ResponseException {
        var path = "/session";
        this.makeRequest("DELETE", path, null, null, authToken);
    }

    public Collection<GameData> listGame(String authToken) throws ResponseException {
        var path = "/game";
        // Record for the game collection
        record ListedGames(Collection<GameData> games) {}
        // Convert return into GameData collection
        return this.makeRequest("GET", path, null, ListedGames.class, authToken).games;
    }

    public int createGame(String authToken, String gameName) throws ResponseException {
        var path = "/game";
        var requestBody = Map.of("gameName", gameName);
        // Record for int response conversion
        record createdGame(int gameID) {}
        return this.makeRequest("POST", path, requestBody, createdGame.class, authToken).gameID;
    }

    public void joinGame(String givenAuthData, ChessGame.TeamColor teamColor, int gameID) throws ResponseException {
        var path = "/game";
        var requestBody = Map.of("playerColor", teamColor, "gameID", gameID);
        this.makeRequest("PUT", path, requestBody, null, givenAuthData);
    }

    public void clearDatabase() throws ResponseException {
        var path = "/db";
        this.makeRequest("DELETE", path, null, null, null);
    }

    // These helper functions were written by examining how the petshop example managed its own helper functions.
    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass, String authToken) throws ResponseException {
        try {
            // set URL connection
            URL reqUrl = (new URI(serverUrl + path)).toURL();
            HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection();
            connection.setRequestMethod(method);
            connection.setDoOutput(true);

            // Set authToken in header
            if (authToken != null) {
                connection.setRequestProperty("Authorization", authToken);
            }

            // turn params into a request object
            writeJsonBody(request, connection);
            connection.connect();
            attemptHttpRequest(connection);
            return readJsonBody(connection, responseClass);

        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseException("Connection error: " + e.getMessage(), 500);
        }
    }

    private <T> T readJsonBody(HttpURLConnection connection, Class<T> responseClass) throws IOException {
        T response = null;
        if (connection.getContentLength() < 0) {
            try (InputStream inputStream = connection.getInputStream()) {
                InputStreamReader reader = new InputStreamReader(inputStream);
                if (responseClass != null) {
                    response = new Gson().fromJson(reader, responseClass);
                }
            }
        }
        return response;
    }

}
