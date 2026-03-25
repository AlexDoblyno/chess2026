package server;

import dataaccess.DataAccessException;
import dataaccess.SqlAuthDataAccess;
import dataaccess.SqlGameDataAccess;
import dataaccess.SqlUserDataAccess;
import models.AuthTokenData;
import models.GameData;
import models.MessageResponse;
import models.UserData;
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
    private Javalin app; // 声明 Javalin 实例，方便后续在 stop() 中调用

    public Server() {
        service = new Service();
        initializeDatabase();
    }

    public int run(int desiredPort) {
        // 初始化 Javalin 并配置静态文件
        app = Javalin.create(config -> {
            config.staticFiles.add("web", Location.CLASSPATH);
        }).start(desiredPort);

        // 注册 endpoints
        app.post("/user", this::registerUser);
        app.post("/session", this::loginUser);
        app.delete("/session", this::logoutUser);
        app.get("/game", this::listGame);
        app.post("/game", this::createGame);
        app.put("/game", this::joinGame);
        app.delete("/db", this::clearDatabase);

        // 注册异常处理（所有抛出的 ServerException 都会走到这里统一处理）
        app.exception(ServerException.class, this::handleException);
        app.exception(Exception.class, this::handleException); // 捕获所有未被处理的其他异常

        return app.port();
    }

    /**
     * registerUser takes the request JSON object, then makes it usable for Java. It then registers the user
     * into the server's database.
     */
    private void registerUser(Context ctx) throws ServerException {
        try {
            // Store the user data from the request
            UserData submittedUser = gson.fromJson(ctx.body(), UserData.class);

            // 1. 先验证对象是否为空，以及字段是否合法。防止 NullPointerException
            if (submittedUser == null ||
                    !validateInput(submittedUser.username()) ||
                    !validateInput(submittedUser.password()) ||
                    !validateEmail(submittedUser.email())) {
                throw new ServerException("bad request", 400);
            }

            // 2. 验证通过后，再安全地进行 trim 操作
            String trimmedUsername = submittedUser.username().trim();
            UserData user = new UserData(trimmedUsername, submittedUser.password(), submittedUser.email());

            // 3. 注册用户并返回 AuthToken
            AuthTokenData authToken = service.register(user);
            ctx.status(200);
            ctx.result(gson.toJson(authToken));

        } catch (JsonSyntaxException e) {
            // 捕获 JSON 解析错误
            throw new ServerException("bad request", 400);
        }
    }

    /**
     * loginUser will attempt to log in the user given a username and password.
     */
    private void loginUser(Context ctx) throws ServerException {
        record UserLoginCredentials(String username, String password) {}

        try {
            UserLoginCredentials userLogin = gson.fromJson(ctx.body(), UserLoginCredentials.class);

            // 检查 null 以防止空请求体导致崩溃
            if (userLogin == null || !validateInput(userLogin.username()) || !validateInput(userLogin.password())) {
                throw new ServerException("bad request", 400);
            }

            AuthTokenData authToken = service.login(userLogin.username(), userLogin.password());
            ctx.status(200);
            ctx.result(gson.toJson(authToken));

        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        }
    }

    /**
     * logoutUser will attempt to log out the user given the session's authtoken.
     */
    private void logoutUser(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        // 这里的 service.logOut 如果遇到无效 token 应该抛出 ServerException(401)
        // 会被底部的 handleException 自动捕获
        service.logOut(authToken);
        ctx.status(200);
        ctx.result("");
    }

    /**
     * listGame will return a list of all current games in the database
     */
    private void listGame(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        Collection<GameData> gameList = service.listGames(authToken);
        ctx.status(200);
        Map<String, Object> jsonMap = Map.of("games", gameList);
        ctx.result(gson.toJson(jsonMap));
    }

    /**
     * createGame will create a new GameData object in the database.
     */
    private void createGame(Context ctx) throws ServerException {
        try {
            Map<String, String> requestBody = gson.fromJson(ctx.body(), Map.class);
            String authToken = ctx.header("authorization");

            // 检查请求体是否为空
            if (requestBody == null || !validateInput(requestBody.get("gameName"))) {
                throw new ServerException("bad request", 400);
            }

            String gameName = requestBody.get("gameName");
            int gameID = service.createGame(authToken, gameName);

            ctx.status(200);
            Map<String, Integer> jsonMap = Map.of("gameID", gameID);
            ctx.result(gson.toJson(jsonMap));

        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        }
    }

    /**
     * joinGame will add a user to an existing GameData object in the database.
     */
    private void joinGame(Context ctx) throws ServerException {
        try {
            Map<String, Object> requestBody = gson.fromJson(ctx.body(), Map.class);
            String authData = ctx.header("authorization");

            // 检查请求体和必须的字段
            if (requestBody != null && validateInput((String)requestBody.get("playerColor")) && requestBody.get("gameID") != null) {
                ChessGame.TeamColor teamColor;
                String colorStr = (String) requestBody.get("playerColor");

                if (colorStr.equalsIgnoreCase("WHITE")) {
                    teamColor = ChessGame.TeamColor.WHITE;
                } else if (colorStr.equalsIgnoreCase("BLACK")){
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
        } catch (JsonSyntaxException | ClassCastException e) {
            throw new ServerException("bad request", 400);
        }
    }

    /**
     * clearDatabase will clear the database.
     */
    private void clearDatabase(Context ctx) throws ServerException {
        service.clearApp();
        ctx.status(200);
        ctx.result("");
    }

    /**
     * 统一处理所有的异常并返回标准化 JSON
     */
    private void handleException(Exception e, Context ctx) {
        int statusCode;
        String errorMessage;

        if (e instanceof ServerException serverException) {
            statusCode = serverException.getStatusCode();
            errorMessage = e.getMessage();
        } else {
            statusCode = 500;
            errorMessage = e.getMessage();
            e.printStackTrace(); // 打印 500 错误的堆栈方便调试
        }

        // 确保错误信息以 "Error: " 开头（如果抛出时没写的话）
        if (!errorMessage.toLowerCase().startsWith("error")) {
            errorMessage = "Error: " + errorMessage;
        }

        MessageResponse messageResponse = new MessageResponse(errorMessage);
        ctx.status(statusCode);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(messageResponse));
    }

    private Boolean validateInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    private Boolean validateEmail(String email) {
        return email != null && !email.trim().isEmpty();
    }

    private void initializeDatabase() {
        try {
            SqlUserDataAccess userDataAccess = new SqlUserDataAccess();
            userDataAccess.configureDatabase();

            SqlGameDataAccess gameDataAccess = new SqlGameDataAccess();
            SqlAuthDataAccess authDataAccess = new SqlAuthDataAccess();

            authDataAccess.configureDatabase();
            gameDataAccess.configureDatabase();
        } catch (dataaccess.ServerException | DataAccessException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage());
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}