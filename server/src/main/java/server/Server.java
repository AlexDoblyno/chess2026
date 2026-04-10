package server;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dataaccess.DataAccessException;
import dataaccess.SqlAuthDataAccess;
import dataaccess.SqlGameDataAccess;
import dataaccess.SqlUserDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import models.AuthTokenData;
import models.GameData;
import models.MessageResponse;
import models.UserData;
import server.websocket.GameWebSocketHandler;
import service.Service;

import java.util.Collection;
import java.util.Map;

public class Server {

    private final Gson gson = new Gson();
    private final Service service = new Service();
    private final GameWebSocketHandler gameWebSocketHandler = new GameWebSocketHandler(service);
    private Javalin app;

    public Server() {
        initializeDatabase();
    }

    public int run(int desiredPort) {
        app = Javalin.create(config -> config.staticFiles.add("web", Location.CLASSPATH));

        registerHttpRoutes();
        registerWebSocketRoutes();
        registerExceptionHandlers();

        app.start(desiredPort);
        return app.port();
    }

    private void registerHttpRoutes() {
        app.post("/user", this::registerUser);
        app.post("/session", this::loginUser);
        app.delete("/session", this::logoutUser);
        app.get("/game", this::listGame);
        app.post("/game", this::createGame);
        app.put("/game", this::joinGame);
        app.delete("/db", this::clearDatabase);
    }

    private void registerWebSocketRoutes() {
        app.ws("/ws", ws -> {
            ws.onMessage(gameWebSocketHandler::onMessage);
            ws.onClose(gameWebSocketHandler::onClose);
            ws.onError(gameWebSocketHandler::onError);
        });
    }

    private void registerExceptionHandlers() {
        app.exception(ServerException.class, (exception, ctx) -> handleException(exception, ctx));
        app.exception(Exception.class, this::handleException);
    }

    private void registerUser(Context ctx) throws ServerException {
        try {
            UserData submittedUser = gson.fromJson(ctx.body(), UserData.class);
            if (submittedUser == null
                    || !validateInput(submittedUser.username())
                    || !validateInput(submittedUser.password())
                    || !validateEmail(submittedUser.email())) {
                throw new ServerException("bad request", 400);
            }

            UserData user = new UserData(submittedUser.username().trim(), submittedUser.password(), submittedUser.email());
            AuthTokenData authToken = service.register(user);

            ctx.status(200);
            ctx.result(gson.toJson(authToken));
        } catch (JsonSyntaxException exception) {
            throw new ServerException("bad request", 400);
        }
    }

    private void loginUser(Context ctx) throws ServerException {
        try {
            LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);
            if (request == null || !validateInput(request.username()) || !validateInput(request.password())) {
                throw new ServerException("bad request", 400);
            }

            AuthTokenData authToken = service.login(request.username(), request.password());
            ctx.status(200);
            ctx.result(gson.toJson(authToken));
        } catch (JsonSyntaxException exception) {
            throw new ServerException("bad request", 400);
        }
    }

    private void logoutUser(Context ctx) throws ServerException {
        service.logOut(ctx.header("authorization"));
        ctx.status(200);
        ctx.result("");
    }

    private void listGame(Context ctx) throws ServerException {
        Collection<GameData> gameList = service.listGames(ctx.header("authorization"));
        ctx.status(200);
        ctx.result(gson.toJson(Map.of("games", gameList)));
    }

    private void createGame(Context ctx) throws ServerException {
        try {
            CreateGameRequest request = gson.fromJson(ctx.body(), CreateGameRequest.class);
            if (request == null || !validateInput(request.gameName())) {
                throw new ServerException("bad request", 400);
            }

            int gameID = service.createGame(ctx.header("authorization"), request.gameName());
            ctx.status(200);
            ctx.result(gson.toJson(Map.of("gameID", gameID)));
        } catch (JsonSyntaxException exception) {
            throw new ServerException("bad request", 400);
        }
    }

    private void joinGame(Context ctx) throws ServerException {
        try {
            JoinGameRequest request = gson.fromJson(ctx.body(), JoinGameRequest.class);
            if (request == null || !validateInput(request.playerColor()) || request.gameID() == null) {
                throw new ServerException("bad request", 400);
            }

            service.joinGame(ctx.header("authorization"), parseTeamColor(request.playerColor()), request.gameID());
            ctx.status(200);
            ctx.result("");
        } catch (IllegalArgumentException | JsonSyntaxException exception) {
            throw new ServerException("bad request", 400);
        }
    }

    private void clearDatabase(Context ctx) throws ServerException {
        service.clearApp();
        gameWebSocketHandler.reset();
        ctx.status(200);
        ctx.result("");
    }

    private void handleException(Exception exception, Context ctx) {
        int statusCode = 500;
        String errorMessage = exception.getMessage();

        if (exception instanceof ServerException serverException) {
            statusCode = serverException.getStatusCode();
        } else {
            exception.printStackTrace();
        }

        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = "internal server error";
        }
        if (!errorMessage.toLowerCase().startsWith("error")) {
            errorMessage = "Error: " + errorMessage;
        }

        ctx.status(statusCode);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(new MessageResponse(errorMessage)));
    }

    private boolean validateInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    private boolean validateEmail(String email) {
        return validateInput(email);
    }

    private ChessGame.TeamColor parseTeamColor(String playerColor) throws ServerException {
        if ("WHITE".equalsIgnoreCase(playerColor)) {
            return ChessGame.TeamColor.WHITE;
        }
        if ("BLACK".equalsIgnoreCase(playerColor)) {
            return ChessGame.TeamColor.BLACK;
        }
        throw new ServerException("bad request", 400);
    }

    private void initializeDatabase() {
        try {
            new SqlUserDataAccess().configureDatabase();
            new SqlGameDataAccess().configureDatabase();
            new SqlAuthDataAccess().configureDatabase();
        } catch (dataaccess.ServerException | DataAccessException exception) {
            throw new RuntimeException("Database initialization failed: " + exception.getMessage(), exception);
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private record LoginRequest(String username, String password) {
    }

    private record CreateGameRequest(String gameName) {
    }

    private record JoinGameRequest(String playerColor, Integer gameID) {
    }
}