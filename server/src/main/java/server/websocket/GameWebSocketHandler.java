package server.websocket;

import chess.ChessGame;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;
import models.AuthTokenData;
import models.GameData;
import server.ServerException;
import service.Service;
import websocket.commands.MakeMoveCommands;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static websocket.messages.ServerMessage.ServerMessageType.ERROR;
import static websocket.messages.ServerMessage.ServerMessageType.LOAD_GAME;
import static websocket.messages.ServerMessage.ServerMessageType.NOTIFICATION;

public class GameWebSocketHandler {

    private final Gson gson = new Gson();
    private final Service service;
    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ConcurrentMap<String, Connection>> gameConnections = new ConcurrentHashMap<>();

    public GameWebSocketHandler(Service service) {
        this.service = service;
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            UserGameCommand command = gson.fromJson(ctx.message(), UserGameCommand.class);
            if (command == null || command.getCommandType() == null || command.getGameID() == null)
                throw new ServerException("bad request", 400);

            refreshConnection(ctx);

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                case MAKE_MOVE -> handleMove(ctx, gson.fromJson(ctx.message(), MakeMoveCommands.class));
                case LEAVE -> handleLeave(ctx, command);
                case RESIGN -> handleResign(ctx, command);
            }
        } catch (JsonSyntaxException exception) {
            sendError(ctx, "bad request");
        } catch (ServerException exception) {
            sendError(ctx, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(ctx, "internal server error");
            removeConnection(ctx.sessionId());
        }
    }

    public void onClose(WsCloseContext ctx) {
        removeConnection(ctx.sessionId());
    }

    public void onError(WsErrorContext ctx) {
        removeConnection(ctx.sessionId());
    }

    public void reset() {
        connections.clear();
        gameConnections.clear();
    }

    private void handleConnect(WsMessageContext ctx, UserGameCommand command) throws ServerException {
        AuthTokenData authData = service.authenticate(command.getAuthToken());
        GameData gameData = service.getGame(command.getGameID());

        addConnection(ctx, gameData.gameID(), authData.username());
        sendToSession(ctx.sessionId(), new LoadGameMessage(LOAD_GAME, gameData.game()));
        sendToOthers(gameData.gameID(), ctx.sessionId(), new NotificationMessage(
                NOTIFICATION,
                connectMessage(gameData, authData.username())
        ));
    }

    private void handleMove(WsMessageContext ctx, MakeMoveCommands command) throws ServerException {
        if (command == null || command.getMove() == null)
            throw new ServerException("bad request", 400);

        AuthTokenData authData = service.authenticate(command.getAuthToken());
        requireConnected(ctx, command.getGameID());

        GameData gameData = service.getGame(command.getGameID());
        ChessGame game = gameData.game();
        if (game.isGameOver())
            throw new ServerException("game over", 400);

        ChessGame.TeamColor playerColor = playerColor(gameData, authData.username());
        if (playerColor == ChessGame.TeamColor.OBSERVE)
            throw new ServerException("observers cannot move", 400);
        if (game.getTeamTurn() != playerColor)
            throw new ServerException("not your turn", 400);

        try {
            game.makeMove(command.getMove());
            game.refreshGameStatus();
        } catch (InvalidMoveException exception) {
            throw new ServerException(exception.getMessage(), 400);
        }

        GameData updatedGame = gameData.setGame(game);
        service.saveGame(updatedGame);

        sendToRootAndOthers(ctx.sessionId(), updatedGame.gameID(), new LoadGameMessage(LOAD_GAME, updatedGame.game()));
        sendToOthers(updatedGame.gameID(), ctx.sessionId(), new NotificationMessage(
                NOTIFICATION,
            authData.username() + " made move " + describeMove(command.getMove()) + "."
        ));

        String statusMessage = buildStatusMessage(updatedGame);
        if (statusMessage != null)
            sendToRootAndOthers(ctx.sessionId(), updatedGame.gameID(), new NotificationMessage(NOTIFICATION, statusMessage));
    }

    private void handleLeave(WsMessageContext ctx, UserGameCommand command) throws ServerException {
        AuthTokenData authData = service.authenticate(command.getAuthToken());
        GameData updatedGame = service.removePlayerFromGame(command.getAuthToken(), command.getGameID());

        removeConnection(ctx.sessionId());
        sendToGame(updatedGame.gameID(), new NotificationMessage(NOTIFICATION, authData.username() + " left the game."));
    }

    private void handleResign(WsMessageContext ctx, UserGameCommand command) throws ServerException {
        AuthTokenData authData = service.authenticate(command.getAuthToken());
        requireConnected(ctx, command.getGameID());

        GameData gameData = service.getGame(command.getGameID());
        ChessGame game = gameData.game();
        if (game.isGameOver())
            throw new ServerException("game over", 400);

        ChessGame.TeamColor playerColor = playerColor(gameData, authData.username());
        if (playerColor == ChessGame.TeamColor.OBSERVE)
            throw new ServerException("observers cannot resign", 400);

        // resign 状态直接落到 game 里，这样重启后还能恢复
        game.markResigned();
        GameData updatedGame = gameData.setGame(game);
        service.saveGame(updatedGame);

        sendToRootAndOthers(ctx.sessionId(), updatedGame.gameID(), new NotificationMessage(
            NOTIFICATION,
            authData.username() + " resigned."
        ));
    }

    private void requireConnected(WsMessageContext ctx, int gameID) throws ServerException {
        Connection connection = connections.get(ctx.sessionId());
        if (connection == null || connection.gameID() != gameID)
            throw new ServerException("not connected to game", 400);
    }

    private void addConnection(WsMessageContext ctx, int gameID, String username) {
        removeConnection(ctx.sessionId());

        Connection connection = new Connection(ctx.sessionId(), gameID, username, ctx);
        connections.put(connection.sessionID(), connection);
        gameConnections.computeIfAbsent(gameID, ignored -> new ConcurrentHashMap<>())
                .put(connection.sessionID(), connection);
    }

    private void refreshConnection(WsMessageContext ctx) {
        Connection connection = connections.get(ctx.sessionId());
        if (connection == null)
            return;

        Connection updatedConnection = new Connection(connection.sessionID(), connection.gameID(), connection.username(), ctx);
        connections.put(updatedConnection.sessionID(), updatedConnection);

        ConcurrentMap<String, Connection> gameConnectionMap = gameConnections.get(updatedConnection.gameID());
        if (gameConnectionMap != null)
            gameConnectionMap.put(updatedConnection.sessionID(), updatedConnection);
    }

    private void removeConnection(String sessionID) {
        Connection connection = connections.remove(sessionID);
        if (connection == null)
            return;

        ConcurrentMap<String, Connection> gameConnectionMap = gameConnections.get(connection.gameID());
        if (gameConnectionMap == null)
            return;

        gameConnectionMap.remove(sessionID);
        if (gameConnectionMap.isEmpty())
            gameConnections.remove(connection.gameID(), gameConnectionMap);
    }

    private void sendToGame(int gameID, ServerMessage message) {
        for (Connection connection : snapshotConnections(gameID))
            send(connection.context(), message);
    }

    private void sendToRootAndOthers(String rootSessionID, int gameID, ServerMessage message) {
        sendToSession(rootSessionID, message);
        sendToOthers(gameID, rootSessionID, message);
    }

    private void sendToSession(String sessionID, ServerMessage message) {
        Connection connection = connections.get(sessionID);
        if (connection != null)
            send(connection.context(), message);
    }

    private void sendToOthers(int gameID, String senderSessionID, ServerMessage message) {
        for (Connection connection : snapshotConnections(gameID)) {
            if (!connection.sessionID().equals(senderSessionID))
                send(connection.context(), message);
        }
    }

    private List<Connection> snapshotConnections(int gameID) {
        ConcurrentMap<String, Connection> gameConnectionMap = gameConnections.get(gameID);
        return gameConnectionMap == null ? List.of() : List.copyOf(gameConnectionMap.values());
    }

    private void send(WsContext ctx, ServerMessage message) {
        try {
            ctx.send(gson.toJson(message));
        } catch (RuntimeException exception) {
            removeConnection(ctx.sessionId());
        }
    }

    private void sendError(WsContext ctx, String errorMessage) {
        send(ctx, new ErrorMessage(ERROR, normalizeErrorMessage(errorMessage)));
    }

    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank())
            return "Error: internal server error";
        return errorMessage.toLowerCase().startsWith("error") ? errorMessage : "Error: " + errorMessage;
    }

    private String connectMessage(GameData gameData, String username) {
        ChessGame.TeamColor teamColor = playerColor(gameData, username);
        if (teamColor == ChessGame.TeamColor.OBSERVE)
            return username + " connected as an observer.";
        return username + " connected as " + describeRole(teamColor) + ".";
    }

    private ChessGame.TeamColor playerColor(GameData gameData, String username) {
        if (username.equals(gameData.whiteUsername()))
            return ChessGame.TeamColor.WHITE;
        if (username.equals(gameData.blackUsername()))
            return ChessGame.TeamColor.BLACK;
        return ChessGame.TeamColor.OBSERVE;
    }

    private String buildStatusMessage(GameData gameData) {
        ChessGame game = gameData.game();
        ChessGame.TeamColor currentTurn = game.getTeamTurn();
        return switch (game.getGameStatus()) {
            case CHECKMATE -> playerLabel(gameData, currentTurn) + " is in checkmate.";
            case STALEMATE -> "The game ended in stalemate.";
            case CHECK -> playerLabel(gameData, currentTurn) + " is in check.";
            default -> null;
        };
    }

    private String playerLabel(GameData gameData, ChessGame.TeamColor teamColor) {
        if (teamColor == ChessGame.TeamColor.WHITE && gameData.whiteUsername() != null)
            return gameData.whiteUsername();
        if (teamColor == ChessGame.TeamColor.BLACK && gameData.blackUsername() != null)
            return gameData.blackUsername();
        return describeRole(teamColor);
    }

    private String describeMove(chess.ChessMove move) {
        StringBuilder description = new StringBuilder();
        description.append(squareName(move.getStartPosition()));
        description.append(" to ");
        description.append(squareName(move.getEndPosition()));

        if (move.getPromotionPiece() != null) {
            description.append(" promoting to ");
            description.append(move.getPromotionPiece().name().toLowerCase());
        }

        return description.toString();
    }

    private String squareName(chess.ChessPosition position) {
        char file = (char) ('a' + position.getColumn() - 1);
        return file + Integer.toString(position.getRow());
    }

    private String describeRole(ChessGame.TeamColor teamColor) {
        return switch (teamColor) {
            case WHITE -> "white";
            case BLACK -> "black";
            case OBSERVE -> "observer";
        };
    }

    private record Connection(String sessionID, int gameID, String username, WsContext context) {
    }
}