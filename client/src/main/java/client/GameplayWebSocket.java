package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import exception.ResponseException;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.net.URI;

public class GameplayWebSocket extends Endpoint {
    public interface GameplayMessageHandler {
        void onLoadGame(LoadGameMessage message);
        void onNotification(NotificationMessage message);
        void onError(ErrorMessage message);
        void onClose(String reason);
    }

    private final URI serverUri;
    private final GameplayMessageHandler messageHandler;
    private final Gson gson = new Gson();
    private Session session;

    public GameplayWebSocket(String serverUrl, GameplayMessageHandler messageHandler) throws ResponseException {
        this.serverUri = buildUri(serverUrl);
        this.messageHandler = messageHandler;
        connect();
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        session.addMessageHandler(String.class, this::handleMessage);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        String reason = closeReason == null || closeReason.getReasonPhrase() == null || closeReason.getReasonPhrase().isBlank()
                ? "Gameplay connection closed."
                : closeReason.getReasonPhrase();
        messageHandler.onClose(reason);
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        String message = throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? "Error: gameplay connection failed"
                : "Error: " + throwable.getMessage();
        messageHandler.onError(new ErrorMessage(ServerMessage.ServerMessageType.ERROR, message));
    }

    public void sendCommand(UserGameCommand command) throws ResponseException {
        if (session == null || !session.isOpen())
            throw new ResponseException("Gameplay connection is not open.", 500);

        session.getAsyncRemote().sendText(gson.toJson(command));
    }

    public void close() {
        if (session == null || !session.isOpen())
            return;

        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    private void connect() throws ResponseException {
        try {
            ClientManager.createClient().connectToServer(this, serverUri);
        } catch (DeploymentException | IOException exception) {
            throw new ResponseException("Unable to connect to gameplay server: " + exception.getMessage(), 500);
        }
    }

    private void handleMessage(String rawMessage) {
        JsonObject envelope = JsonParser.parseString(rawMessage).getAsJsonObject();
        ServerMessage.ServerMessageType type = ServerMessage.ServerMessageType.valueOf(
                envelope.get("serverMessageType").getAsString());

        switch (type) {
            case LOAD_GAME -> messageHandler.onLoadGame(gson.fromJson(rawMessage, LoadGameMessage.class));
            case NOTIFICATION -> messageHandler.onNotification(gson.fromJson(rawMessage, NotificationMessage.class));
            case ERROR -> messageHandler.onError(gson.fromJson(rawMessage, ErrorMessage.class));
        }
    }

    private static URI buildUri(String serverUrl) throws ResponseException {
        try {
            String wsUrl = serverUrl.replaceFirst("^http", "ws");
            if (!wsUrl.endsWith("/"))
                wsUrl += "/";
            return URI.create(wsUrl + "ws");
        } catch (IllegalArgumentException exception) {
            throw new ResponseException("Invalid server URL: " + serverUrl, 500);
        }
    }
}