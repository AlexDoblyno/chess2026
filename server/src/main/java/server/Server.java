package server;

import Models.AuthTokenData;
import Models.GameData;
import Models.MessageResponse;
import Models.UserData;
import chess.ChessGame;
import com.google.gson.JsonSyntaxException;
import service.Service;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.util.Collection;
import java.util.Map;

public class Server {

    private final Gson gson = new Gson();
    private final Service service;
    private Javalin app; // 保存 Javalin 实例以便后续可以调用 stop()

    public Server() {
        service = new Service();
    }

    public int run(int desiredPort) {
        // 1. 初始化 Javalin 并配置静态文件
        app = Javalin.create(config -> {
            config.staticFiles.add("/web", Location.CLASSPATH);
        });

        // 2. 注册路由 (Endpoints)
        app.post("/user", this::registerUser);
        app.post("/session", this::loginUser);
        app.delete("/session", this::logoutUser);
        app.get("/game", this::listGame);
        app.post("/game", this::createGame);
        app.put("/game", this::joinGame);
        app.delete("/db", this::clearDatabase);

        // 3. 注册异常处理器
        app.exception(ServerException.class, this::handleException);
        // 捕获所有其他未预料的异常作为 500 错误
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(new MessageResponse("Error: " + e.getMessage())));
        });

        // 4. 启动服务器
        app.start(desiredPort);
        return app.port();
    }

    /**
     * registerUser
     */
    private void registerUser(Context ctx) throws ServerException {
        try {
            UserData submittedUser = gson.fromJson(ctx.body(), UserData.class);

            String trimmedUsername = submittedUser.username().trim();
            UserData user = new UserData(trimmedUsername, submittedUser.password(), submittedUser.email());

            if (!validateInput(user.username()) || !validateInput(user.password()) || !validateEmail(user.email())) {
                throw new ServerException("bad request", 400);
            } else {
                AuthTokenData authToken = service.register(user);
                ctx.status(200);
                ctx.result(gson.toJson(authToken));
            }
        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        }
    }

    /**
     * loginUser
     */
    private void loginUser(Context ctx) throws ServerException {
        record UserLoginCredentials(String username, String password) {}

        UserLoginCredentials userLogin = gson.fromJson(ctx.body(), UserLoginCredentials.class);
        String username = userLogin.username;
        String password = userLogin.password;

        try {
            if (username == null || password == null) {
                throw new ServerException("bad request", 400);
            }
            AuthTokenData authToken = service.login(username, password);
            ctx.status(200);
            ctx.result(gson.toJson(authToken));
        } catch(ServerException e) {
            ctx.status(e.getStatusCode());
            ctx.result(gson.toJson(new MessageResponse("Error: " + e.getMessage())));
        }
    }

    /**
     * logoutUser
     */
    private void logoutUser(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        service.logOut(authToken);
        ctx.status(200);
        ctx.result(""); // 空字符串响应
    }

    /**
     * listGame
     */
    private void listGame(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        Collection<GameData> gameList = service.listGames(authToken);
        ctx.status(200);
        Map<String, Object> jsonMap = Map.of("games", gameList);
        ctx.result(gson.toJson(jsonMap));
    }

    /**
     * createGame
     */
    private void createGame(Context ctx) throws ServerException {
        Map<String, String> requestBody = gson.fromJson(ctx.body(), Map.class);
        String authToken = ctx.header("authorization");
        String gameName = requestBody.get("gameName");

        if (gameName == null || gameName.trim().isEmpty()) {
            ctx.status(400);
            ctx.result(gson.toJson(new MessageResponse("Error: Bad request - gameName is required")));
            return;
        }

        int gameID = service.createGame(authToken, gameName);
        ctx.status(200);
        Map<String, Integer> jsonMap = Map.of("gameID", gameID);
        ctx.result(gson.toJson(jsonMap));
    }

    /**
     * joinGame
     */
    private void joinGame(Context ctx) throws ServerException {
        Map<String, Object> requestBody = gson.fromJson(ctx.body(), Map.class);
        ChessGame.TeamColor teamColor;
        String authData = ctx.header("authorization");

        if (validateInput((String)requestBody.get("playerColor")) && requestBody.get("gameID") != null) {
            if (((String) requestBody.get("playerColor")).equalsIgnoreCase("WHITE")) {
                teamColor = ChessGame.TeamColor.WHITE;
            } else if (((String) requestBody.get("playerColor")).equalsIgnoreCase("BLACK")){
                teamColor = ChessGame.TeamColor.BLACK;
            } else {
                throw new ServerException("bad request", 400);
            }
            double gameIDDouble = (double) requestBody.get("gameID");
            int gameID = (int) gameIDDouble;

            service.joinGame(authData, teamColor, gameID);
            ctx.status(200);
            ctx.result("");
        } else {
            throw new ServerException("bad request", 400);
        }
    }

    /**
     * clearDatabase
     */
    private void clearDatabase(Context ctx) {
        service.clearApp();
        ctx.status(200);
        ctx.result("");
    }

    /**
     * 专门用于处理 ServerException 的异常处理器
     */
    private void handleException(ServerException e, Context ctx) {
        int statusCode = e.getStatusCode();
        System.out.println(statusCode);
        String errorMessage = "Error: " + e.getMessage();
        MessageResponse messageResponse = new MessageResponse(errorMessage);

        ctx.status(statusCode);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(messageResponse));
    }

    private Boolean validateInput(String input) {
        return input != null && !input.isEmpty();
    }

    private Boolean validateEmail(String email) {
        if (email != null) {
            return true;
        }
        return false;
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}