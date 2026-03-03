package ui;

import chess.ChessGame;
import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import javax.websocket.*;
import java.net.URI;

/**
 * WebSocketClient 负责：
 * <ul>
 *     <li>与指定 WebSocket 服务器建立连接并发送消息</li>
 *     <li>接收服务器的 LOAD_GAME / NOTIFICATION / ERROR 等类型的消息</li>
 *     <li>后续可支持断线重连等功能</li>
 * </ul>
 *
 * <p>参考：
 * <a href="https://docs.oracle.com/javaee/7/api/javax/websocket/ClientEndpoint.html">
 * javax.websocket.ClientEndpoint 文档</a>
 */
@ClientEndpoint
public class WebSocketClient {

    /**
     * 当前WebSocket连接的会话
     */
    private Session session;

    /**
     * Gson实例，用于序列化/反序列化JSON
     */
    private final Gson gson = new Gson();

    /**
     * 客户端暂存的ChessGame对象
     */
    private ChessGame game;

    /**
     * 当前客户端所处的队伍颜色(默认白方，可覆盖)
     */
    public ChessGame.TeamColor teamColor = ChessGame.TeamColor.WHITE;

    /**
     * 该客户端所对应的GameUI实例
     */
    private final GameUI gameUI;

    /**
     * 构造函数：初始化并尝试连接WebSocket服务器
     *
     * @param serverUri WebSocket服务器地址
     * @param gameUI    UI界面实例，用于更新或显示游戏数据
     */
    public WebSocketClient(String serverUri, GameUI gameUI) {
        this.gameUI = gameUI;
        connect(serverUri);
    }

    /**
     * 连接到指定的WebSocket服务器
     *
     * @param serverUri 服务器WebSocket地址(ws://或wss://)
     */
    private void connect(String serverUri) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(serverUri));
            System.out.println("Connected to WebSocket server at: " + serverUri);
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            // 可在此处添加重试逻辑或进一步处理
        }
    }

    /**
     * 当WebSocket连接建立时自动调用
     *
     * @param session 建立连接之后的会话对象
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
    }

    /**
     * 当收到服务器消息时，根据其类型进行不同处理
     *
     * @param message 服务器发送的JSON文本
     */
    @OnMessage
    public void onMessage(String message) {
        if (message.contains("LOAD_GAME")) {
            LoadGameMessage msg = gson.fromJson(message, LoadGameMessage.class);
            handleLoadGame(msg.getGame());
        } else if (message.contains("NOTIFICATION")) {
            NotificationMessage msg = gson.fromJson(message, NotificationMessage.class);
            handleNotification(msg.getMessage());
        } else if (message.contains("ERROR")) {
            ErrorMessage msg = gson.fromJson(message, ErrorMessage.class);
            handleError(msg.getErrorMessage());
        } else {
            System.err.println("Unknown server message type.");
        }
        System.out.print("\r[IN_GAME] >>> ");
    }

    /**
     * 向服务器发送指令(UserGameCommand)
     *
     * @param command 待发送的命令对象
     */
    public void sendMessage(UserGameCommand command) {
        try {
            if (session != null && session.isOpen()) {
                String json = gson.toJson(command);
                session.getBasicRemote().sendText(json);
            } else {
                System.err.println("Cannot send message: WebSocket session is closed.");
            }
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * 主动关闭WebSocket连接
     */
    public void close() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to close WebSocket: " + e.getMessage());
        }
    }

    /**
     * 处理服务器发送的LOAD_GAME消息
     *
     * @param game 服务器返回的ChessGame对象
     */
    private void handleLoadGame(ChessGame game) {
        this.game = game;
        gameUI.loadGame(game);
    }

    /**
     * 处理服务器发送的ERROR消息
     *
     * @param errorMessage 服务器返回的错误信息
     */
    private void handleError(String errorMessage) {
        System.err.println("Error from server: " + errorMessage);
    }

    /**
     * 处理服务器发送的NOTIFICATION消息
     *
     * @param message 提示消息内容
     */
    private void handleNotification(String message) {
        System.out.println("\rNotification: " + message);
    }
}