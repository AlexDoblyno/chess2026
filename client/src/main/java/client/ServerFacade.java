package client;

import chess.ChessGame;
import com.google.gson.Gson;
import models.*;
import exception.ResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
        // Convert return into GameData collection 将返回值转换为 GameData 集合
        return this.makeRequest("GET", path, null, ListedGames.class, authToken).games;
    }

    public int createGame(String authToken, String gameName) throws ResponseException {
        var path = "/game";
        var requestBody = Map.of("gameName", gameName);
        // int 响应转换记录
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
    //  参考了前任petshop，    这些辅助函数是通过研究 petshop 示例如何管理自己的辅助函数编写的。
    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass, String authToken) throws ResponseException {
        try {
            // 设置 URL 连接
            URL reqUrl = (new URI(serverUrl + path)).toURL();
            HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection();
            connection.setRequestMethod(method);
            connection.setDoOutput(true);

            // Set authToken
            if (authToken != null) {
                connection.setRequestProperty("Authorization", authToken);
            }

            // turn params into a request object 将参数转化为请求对象
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

    private void attemptHttpRequest(HttpURLConnection connection) throws ResponseException, IOException {
        int statusCode = connection.getResponseCode();
        // Ensure status code is in the 200 range. If it doesn't, we figure out what to throw.
        //确保状态代码在 200 范围内。如果不在 200 范围内，我们就要想办法解决报错。
        if (statusCode / 100 != 2) {
            try (InputStream errorStream = connection.getErrorStream()) {
                if (errorStream != null) {
                    throw ResponseException.fromJson(errorStream);
                }
            }
            throw new ResponseException("Other error: " + statusCode, statusCode);
        }
    }

    private static void writeJsonBody(Object request, HttpURLConnection connection) throws IOException {
        if (request != null) {
            connection.addRequestProperty("Content-Type", "application/json");
            String jsonRequest = new Gson().toJson(request);
            try (OutputStream requestBody = connection.getOutputStream()) {
                requestBody.write(jsonRequest.getBytes());
            }
        }
    }
}
