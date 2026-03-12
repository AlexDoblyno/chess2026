package server;

import dataaccess.DataAccessException;
import dataaccess.SqlAuthDataAccess;
import dataaccess.SqlGameDataAccess;
import dataaccess.SqlUserDataAccess;
import Models.AuthTokenData;
import Models.GameData;
import Models.MessageResponse;
import Models.UserData;
import chess.ChessGame;
import com.google.gson.JsonSyntaxException;
import Service.Service;
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

        // 注册异常处理
        app.exception(ServerException.class, this::handleException);
        app.exception(Exception.class, this::handleException); // 捕获所有未被处理的其他异常

        return app.port();
    }

    /**
     * registerUser takes the request JSON object, then makes it usable for Java. It then registers the user
     * into the server's database.
     */
    private void registerUser(Context ctx) {
        try {
            // Store the user data from the request
            UserData submittedUser = gson.fromJson(ctx.body(), UserData.class);

            // Trim the username
            String trimmedUsername = submittedUser.username().trim();
            UserData user = new UserData(trimmedUsername, submittedUser.password(), submittedUser.email());

            // Verify inputs
            if (!validateInput(user.username()) || !validateInput(user.password()) || !validateEmail(user.email())) {
                throw new ServerException("Error: bad request", 400);
            }

            // Register user data
            AuthTokenData authToken = service.register(user);
            ctx.status(200);
            ctx.result(gson.toJson(authToken));

        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            createErrorResponse(ctx, "Error! bad request", 400);
        } catch (ServerException e) {
            e.printStackTrace();
            createErrorResponse(ctx, "error| " + e.getMessage(), e.getStatusCode());
        }
    }

    // 通用的方法来创建错误响应
    private void createErrorResponse(Context ctx, String errorMessage, int statusCode) {
        ctx.status(statusCode);
        ctx.result(gson.toJson(new MessageResponse(errorMessage)));
    }

    /**
     * loginUser will attempt to log in the user given a username and password.
     */
    private void loginUser(Context ctx) throws ServerException {
        record UserLoginCredentials(String username, String password) {}

        UserLoginCredentials userLogin = gson.fromJson(ctx.body(), UserLoginCredentials.class);
        if (!validateInput(userLogin.username()) || !validateInput(userLogin.password())) {
            throw new ServerException("bad request", 400);
        }

        try {
            AuthTokenData authToken = service.login(userLogin.username(), userLogin.password());
            ctx.status(200);
            ctx.result(gson.toJson(authToken));
        } catch(ServerException e) {
            ctx.status(e.getStatusCode());
            ctx.result(gson.toJson(new MessageResponse("Error: " + e.getMessage())));
        }
    }

    /**
     * logoutUser will attempt to log out the user given the session's authtoken.
     */
    private void logoutUser(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        try {
            service.logOut(authToken);
            ctx.status(200);
            ctx.result("");
        } catch (ServerException e) {
            e.printStackTrace();
            createErrorResponse(ctx, "error| " + e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * listGame will return a list of all current games in the database
     */
    private void listGame(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");
        try {
            Collection<GameData> gameList = service.listGames(authToken);
            ctx.status(200);
            Map<String, Object> jsonMap = Map.of("games", gameList);
            ctx.result(gson.toJson(jsonMap));
        } catch (ServerException e) {
            throw new ServerException("Error: " + e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * createGame will create a new GameData object in the database.
     */
    private void createGame(Context ctx) throws ServerException {
        Map<String, String> requestBody = gson.fromJson(ctx.body(), Map.class);
        String authToken = ctx.header("authorization");
        String gameName = requestBody.get("gameName");

        try {
            int gameID = service.createGame(authToken, gameName);
            ctx.status(200);
            Map<String, Integer> jsonMap = Map.of("gameID", gameID);
            ctx.result(gson.toJson(jsonMap));
        } catch (ServerException e) {
            createErrorResponse(ctx, e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * joinGame will add a user to an existing GameData object in the database.
     */
    private void joinGame(Context ctx) throws ServerException {
        Map<String, Object> requestBody = gson.fromJson(ctx.body(), Map.class);
        String authData = ctx.header("authorization");

        if (validateInput((String)requestBody.get("playerColor")) && requestBody.get("gameID") != null) {
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
     * Method to handle exceptions
     */
    private void handleException(Exception e, Context ctx) {
        int statusCode;
        String errorMessage;

        if (e instanceof ServerException serverException) {
            statusCode = serverException.getStatusCode();
            errorMessage = "Error: " + e.getMessage();
        } else {
            statusCode = 500;
            errorMessage = "Error: " + e.getMessage();
        }

        MessageResponse messageResponse = new MessageResponse(errorMessage);
        ctx.status(statusCode);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(messageResponse));
    }

    private Boolean validateInput(String input) {
        return input != null && !input.isEmpty();
    }

    private Boolean validateEmail(String email) {
        return email != null;
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
//总共修改javalin，不知道test有没有问题，先提交