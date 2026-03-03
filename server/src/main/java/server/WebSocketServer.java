package server;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.AuthDataAccess;
import dataaccess.DataAccessException;
import dataaccess.GameDataAccess;
import dataaccess.ServerException;
import dataaccess.UserDataAccess;
import models.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.MakeMoveCommands;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.*;

/**
 * This WebSocketServer handles incoming WebSocket requests:
 * <ul>
 *     <li>CONNECT / LEAVE / RESIGN commands (UserGameCommand)</li>
 *     <li>MAKE_MOVE commands (MakeMoveCommands)</li>
 * </ul>
 *
 * <p>部分核心思路参考：
 * <a href="https://docs.oracle.com/javaee/7/api/javax/websocket/OnMessage.html">Java WebSocket OnMessage官方文档</a>
 */
@WebSocket
public class WebSocketServer {

    private final UserDataAccess userDA;
    private final GameDataAccess gameDA;
    private final AuthDataAccess authDA;

    /**
     * 每个游戏ID和对应的WebSocket会话映射表
     */
    private static final Map<Integer, List<Session>> SESSIONS = new HashMap<>();

    /**
     * 构造函数，注入数据访问对象
     *
     * @param authDA Auth数据访问接口
     * @param gameDA 游戏数据访问接口
     * @param userDA 用户数据访问接口
     */
    public WebSocketServer(AuthDataAccess authDA, GameDataAccess gameDA, UserDataAccess userDA) {
        this.userDA = userDA;
        this.gameDA = gameDA;
        this.authDA = authDA;
    }

    /**
     * WebSocket建立连接时调用
     *
     * @param session 新连接的会话
     */
    @OnWebSocketConnect
    public void onOpen(Session session) {
        System.out.println("New connection");
    }

    /**
     * 收到消息时执行，根据消息做相应处理
     *
     * @param session 当前WebSocket会话
     * @param message 接收到的json文本
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        System.out.println("New message: " + message);

        // 如果消息表示移动棋子
        if (message.contains("MAKE_MOVE")) {
            MakeMoveCommands moveCommand = new Gson().fromJson(message, MakeMoveCommands.class);
            try {
                handleMakeMove(moveCommand, session);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error: " + e.getMessage());
            }
            return;
        }

        // 否则为UserGameCommand类型
        UserGameCommand command = parseCommand(message);
        try {
            handleCommand(command, session);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private UserGameCommand parseCommand(String message) {
        return new Gson().fromJson(message, UserGameCommand.class);
    }

    /**
     * 判断并执行具体的CONNECT、LEAVE、RESIGN三种命令
     */
    private void handleCommand(UserGameCommand command, Session session) throws DataAccessException {
        switch (command.getCommandType()) {
            case CONNECT -> handleConnect(command, session);
            case LEAVE -> handleLeave(command, session);
            case RESIGN -> handleResign(command, session);
            default -> System.out.println("Unknown command type: " + command.getCommandType());
        }
    }

    /**
     * 处理CONNECT命令，连接到游戏并通知其他玩家
     */
    private void handleConnect(UserGameCommand command, Session session) throws DataAccessException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username;

        // 验证用户令牌
        try {
            username = authDA.getUsername(authToken);
        } catch (ServerException e) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid authToken");
            sendMessage(new Gson().toJson(msg), session);
            throw new DataAccessException(e.getMessage());
        }

        // 获取游戏数据
        GameData gameData;
        try {
            gameData = gameDA.getGameByID(gameID);
        } catch (ServerException e) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
            sendMessage(new Gson().toJson(msg), session);
            throw new DataAccessException(e.getMessage());
        }

        // 双重判断游戏与玩家有效性
        try {
            if (authDA.getUsername(authToken) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid authToken");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
            if (gameDA.getGameByID(gameID) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 登记Session
        SESSIONS.computeIfAbsent(gameID, k -> new ArrayList<>()).add(session);

        // 判断颜色
        String color = "observer";
        if (Objects.equals(gameData.whiteUsername(), username)) {
            color = "white";
        } else if (Objects.equals(gameData.blackUsername(), username)) {
            color = "black";
        }

        // 通知其他玩家
        NotificationMessage notification = new NotificationMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                username + " has joined the game as " + color);
        broadcastMessageExclude(new Gson().toJson(notification), gameID, session);

        // 给当前连接玩家发送游戏信息
        LoadGameMessage gameMessage = new LoadGameMessage(ServerMessage.ServerMessageType.LOAD_GAME, gameData.game());
        sendMessage(new Gson().toJson(gameMessage), session);
    }

    /**
     * 处理MAKE_MOVE命令，执行走棋并通知其他玩家
     */
    private void handleMakeMove(MakeMoveCommands command, Session session) throws DataAccessException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();

        // 获取游戏数据
        GameData gameData;
        try {
            gameData = gameDA.getGameByID(gameID);
        } catch (ServerException e) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
            sendMessage(new Gson().toJson(msg), session);
            throw new DataAccessException(e.getMessage());
        }
        ChessMove move = command.getMove();

        // 验证AuthToken存在
        try {
            if (authDA.getAuthData(authToken) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid authToken");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid authToken");
            sendMessage(new Gson().toJson(msg), session);
            throw new DataAccessException(e.getMessage());
        }

        // 验证是否是当前玩家的回合
        try {
            String tokenUser = authDA.getUsername(authToken);
            if (!isCorrectTurn(tokenUser, gameData)) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "wrong turn");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 验证游戏是否存在
        try {
            if (gameDA.getGameByID(gameID) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
            sendMessage(new Gson().toJson(msg), session);
            throw new DataAccessException(e.getMessage());
        }

        // 检查游戏是否已经结束
        if (gameData.game().isOver()) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game is over");
            sendMessage(new Gson().toJson(msg), session);
            return;
        }

        // 进行走棋操作
        ChessGame game = gameData.game();
        ChessGame.TeamColor currentColor = game.getTeamTurn();
        String username;
        try {
            username = authDA.getUsername(authToken);
            game.makeMove(move);
        } catch (InvalidMoveException ivm) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid move");
            sendMessage(new Gson().toJson(msg), session);
            return;
        } catch (ServerException e) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid move");
            sendMessage(new Gson().toJson(msg), session);
            throw new DataAccessException(e.getMessage());
        }

        // 持久化更新
        try {
            gameDA.updateChessGame(game, gameID);
        } catch (DataAccessException e) {
            // 根据需求可在此处处理异常或继续抛出
            throw e;
        }

        // 广播新的游戏状态
        LoadGameMessage msgLoad = new LoadGameMessage(ServerMessage.ServerMessageType.LOAD_GAME, game);
        broadcastMessage(new Gson().toJson(msgLoad), gameID);

        // 通知其他玩家当前走棋
        NotificationMessage moveMsg = new NotificationMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                currentColor + " user " + username + " has made a move from " +
                        formatPosition(move.getStartPosition()) + " to " + formatPosition(move.getEndPosition()));
        broadcastMessageExclude(new Gson().toJson(moveMsg), gameID, session);

        // 检查是否将死/将军/僵局
        checkGameEndingState(game, gameID, currentColor, username, session, gameData);
    }

    private boolean isCorrectTurn(String tokenUser, GameData gameData) {
        ChessGame.TeamColor teamTurn = gameData.game().getTeamTurn();
        return (tokenUser.equals(gameData.blackUsername()) && teamTurn == ChessGame.TeamColor.BLACK)
                || (tokenUser.equals(gameData.whiteUsername()) && teamTurn == ChessGame.TeamColor.WHITE);
    }

    // 检查游戏状态是否终盘，如checkmate或stalemate
    private void checkGameEndingState(ChessGame game, int gameID, ChessGame.TeamColor color,
                                      String currentUser, Session session, GameData gameData) {
        ChessGame.TeamColor opponent = (color == ChessGame.TeamColor.WHITE)
                ? ChessGame.TeamColor.BLACK
                : ChessGame.TeamColor.WHITE;
        String opponentName = (opponent == ChessGame.TeamColor.WHITE)
                ? gameData.whiteUsername()
                : gameData.blackUsername();

        if (game.isInCheckmate(opponent)) {
            NotificationMessage note = new NotificationMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    opponent + " user " + opponentName + " is in checkmate, "
                            + color + " user " + currentUser + " wins");
            broadcastMessage(new Gson().toJson(note), gameID);
        } else if (game.isInCheck(opponent)) {
            NotificationMessage note = new NotificationMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    opponent + " user " + opponentName + " is in check");
            broadcastMessage(new Gson().toJson(note), gameID);
            game.setGameOverStatus(true);
        } else if (game.isInStalemate(opponent) || game.isInStalemate(color)) {
            NotificationMessage note = new NotificationMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    "game ends in stalemate");
            broadcastMessage(new Gson().toJson(note), gameID);
            game.setGameOverStatus(true);
        }
    }

    private int formatPosition(ChessPosition position) {
        char col = (char) ('a' + position.getColumn() - 1);
        return col + position.getRow();
    }

    /**
     * 处理LEAVE命令，玩家离开游戏
     */
    private void handleLeave(UserGameCommand command, Session session) throws DataAccessException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username;
        GameData gameData;

        try {
            username = authDA.getUsername(authToken);
            gameData = gameDA.getGameByID(gameID);
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 验证authToken有效性
        try {
            if (authDA.getAuthData(authToken) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid authToken");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 验证游戏是否存在
        try {
            if (gameDA.getGameByID(gameID) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 判断角色
        String color = "observer";
        if (Objects.equals(gameData.whiteUsername(), username)) {
            color = "white";
        } else if (Objects.equals(gameData.blackUsername(), username)) {
            color = "black";
        }

        // 通知离开
        NotificationMessage note = new NotificationMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                color + " user " + username + " has left the game");
        broadcastMessageExclude(new Gson().toJson(note), gameID, session);

        // 如果是玩家，则将该玩家置空
        if (!"observer".equals(color)) {
            ChessGame.TeamColor teamColor = color.equals("white") ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
            gameDA.updateGame(teamColor, gameID, null);
        }

        SESSIONS.getOrDefault(gameID, Collections.emptyList()).remove(session);
    }

    /**
     * 处理RESIGN命令，玩家认输
     */
    private void handleResign(UserGameCommand command, Session session) throws DataAccessException {
        Integer gameID = command.getGameID();
        String authToken = command.getAuthToken();
        String username;

        // 获取玩家与游戏
        try {
            username = authDA.getUsername(authToken);
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }
        GameData gameData;
        try {
            gameData = gameDA.getGameByID(gameID);
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 检查是否是对局中的玩家
        if (!username.equals(gameData.blackUsername()) && !username.equals(gameData.whiteUsername())) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "wrong turn");
            sendMessage(new Gson().toJson(msg), session);
            return;
        }

        // 验证authToken与游戏有效性
        try {
            if (authDA.getUsername(authToken) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "invalid authToken");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
            if (gameDA.getGameByID(gameID) == null) {
                ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game not found");
                sendMessage(new Gson().toJson(msg), session);
                return;
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 若游戏已结束
        if (gameData.game().isOver()) {
            ErrorMessage msg = new ErrorMessage(ServerMessage.ServerMessageType.ERROR, "game is over");
            sendMessage(new Gson().toJson(msg), session);
        } else {
            // 正式执行认输
            ChessGame game = gameData.game();
            ChessGame.TeamColor color = (username.equals(gameData.whiteUsername()))
                    ? ChessGame.TeamColor.WHITE
                    : ChessGame.TeamColor.BLACK;
            ChessGame.TeamColor opponent = (color == ChessGame.TeamColor.WHITE)
                    ? ChessGame.TeamColor.BLACK
                    : ChessGame.TeamColor.WHITE;
            String opponentName = (opponent == ChessGame.TeamColor.WHITE)
                    ? gameData.whiteUsername()
                    : gameData.blackUsername();

            NotificationMessage note = new NotificationMessage(
                    ServerMessage.ServerMessageType.NOTIFICATION,
                    color + " user " + username + " has resigned, "
                            + opponent + " user " + opponentName + " wins");
            broadcastMessage(new Gson().toJson(note), gameID);

            // 标记游戏结束并更新数据库
            game.setGameOverStatus(true);
            gameDA.updateChessGame(game, gameID);
        }
    }

    /**
     * WebSocket异常监听
     */
    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
        System.out.println("Error: " + throwable.getMessage());
    }

    /**
     * WebSocket关闭连接时
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("Connection closed, reason: " + reason);
    }

    /**
     * 广播消息给指定游戏所有在线玩家
     */
    private void broadcastMessage(String msg, Integer gameID) {
        List<Session> gameSessions = SESSIONS.get(gameID);
        if (gameSessions != null) {
            for (Session s : gameSessions) {
                if (s.isOpen()) {
                    sendMessage(msg, s);
                }
            }
        }
    }

    /**
     * 广播消息给游戏中除特定会话外的所有在线玩家
     */
    private void broadcastMessageExclude(String msg, Integer gameID, Session exclude) {
        List<Session> gameSessions = SESSIONS.get(gameID);
        if (gameSessions != null) {
            for (Session s : gameSessions) {
                if (!s.equals(exclude) && s.isOpen()) {
                    sendMessage(msg, s);
                }
            }
        }
    }

    /**
     * 发送消息给特定会话
     */
    private void sendMessage(String message, Session session) {
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }
}