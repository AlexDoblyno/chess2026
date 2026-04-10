package ui;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import client.ChessClient;
import client.GameplayWebSocket;
import com.google.gson.Gson;
import exception.ResponseException;
import exception.UIStateException;
import models.GameData;
import websocket.commands.MakeMoveCommands;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GameUI extends BaseUI implements GameplayWebSocket.GameplayMessageHandler {
    private static final Object CONSOLE_LOCK = new Object();

    private final boolean isPlayer;
    private final ChessboardDrawer drawer;
    private final ChessGame.TeamColor perspective;
    private final int gameID;
    private final String gameName;
    private final GameplayWebSocket gameplaySocket;
    private volatile boolean closingLocally;

    public GameUI(ChessClient client, GameData gameData, ChessGame.TeamColor perspective,
                  boolean isPlayer) throws ResponseException {
        super(client);
        state = UIStatesEnum.GAMEUI;
        this.isPlayer = isPlayer;
        this.perspective = perspective == ChessGame.TeamColor.BLACK ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        this.gameID = gameData.gameID();
        this.gameName = gameData.gameName();
        this.drawer = new ChessboardDrawer(gameData.game(), this.perspective);
        this.gameplaySocket = new GameplayWebSocket(client.getServerURL(), this);
        gameplaySocket.sendCommand(new UserGameCommand(
                UserGameCommand.CommandType.CONNECT,
                client.getDataCache().getAuthToken(),
                gameID
        ));
    }

    @Override
    public String handler(String input) throws ResponseException {
        String[] tokens = tokenizeInput(input);

        return switch (tokens[0].toLowerCase()) {
            case "help" -> displayHelpInfo();
            case "redraw" -> renderBoard();
            case "highlight" -> highlightMoves(tokens);
            case "move" -> makeMove(tokens);
            case "resign" -> resign(tokens);
            case "leave", "exit" -> leaveGame();
            default -> displayHelpInfo();
        };
    }

    public String initialScreen() {
        return renderBoard();
    }

    private String renderBoard() {
        return drawer.drawBoardString() + footerText();
    }

    private String highlightMoves(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        ChessPosition startPosition = parsePosition(tokens[1]);
        ChessPiece selectedPiece = drawer.getChessGame().getBoard().getPiece(startPosition);
        if (selectedPiece == null)
            throw new ResponseException("There is no piece on " + formatPosition(startPosition) + ".", 400);

        Gson gson = new Gson();
        ChessGame previewGame = gson.fromJson(gson.toJson(drawer.getChessGame()), ChessGame.class);
        previewGame.setTeamTurn(selectedPiece.getTeamColor());
        Collection<ChessMove> validMoves = previewGame.validMoves(startPosition);

        Set<ChessPosition> highlightSquares = new HashSet<>();
        if (validMoves != null) {
            for (ChessMove move : validMoves)
                highlightSquares.add(move.getEndPosition());
        }

        return drawer.drawBoardString(highlightSquares, startPosition)
                + "Highlighted legal moves for " + formatPosition(startPosition) + ".\n"
                + footerText();
    }

    private String makeMove(String[] tokens) throws ResponseException {
        if (!isPlayer)
            throw new ResponseException("Observers cannot make moves.", 400);
        if (tokens.length < 3 || tokens.length > 4)
            throw new ResponseException("Use: move <from> <to> [promotion]", 400);

        ChessPosition startPosition = parsePosition(tokens[1]);
        ChessPosition endPosition = parsePosition(tokens[2]);
        ChessPiece movingPiece = drawer.getChessGame().getBoard().getPiece(startPosition);
        if (movingPiece == null)
            throw new ResponseException("There is no piece on " + formatPosition(startPosition) + ".", 400);
        if (movingPiece.getTeamColor() != perspective)
            throw new ResponseException("That piece is not yours to move.", 400);

        ChessPiece.PieceType promotionPiece = tokens.length == 4 ? parsePromotion(tokens[3]) : null;
        ChessMove move = new ChessMove(startPosition, endPosition, promotionPiece);
        gameplaySocket.sendCommand(new MakeMoveCommands(
                UserGameCommand.CommandType.MAKE_MOVE,
                client.getDataCache().getAuthToken(),
                gameID,
                move
        ));
        return "Move request sent: " + formatPosition(startPosition) + " -> " + formatPosition(endPosition) + ".";
    }

    private String resign(String[] tokens) throws ResponseException {
        if (!isPlayer)
            throw new ResponseException("Observers cannot resign.", 400);
        if (tokens.length == 1)
            return "Type 'resign yes' to confirm, or anything else to cancel.";
        if (tokens.length == 2 && (tokens[1].equalsIgnoreCase("yes") || tokens[1].equalsIgnoreCase("confirm"))) {
            gameplaySocket.sendCommand(new UserGameCommand(
                    UserGameCommand.CommandType.RESIGN,
                    client.getDataCache().getAuthToken(),
                    gameID
            ));
            return "Resignation sent.";
        }
        return "Resign canceled.";
    }

    private String leaveGame() throws ResponseException {
        try {
            gameplaySocket.sendCommand(new UserGameCommand(
                    UserGameCommand.CommandType.LEAVE,
                    client.getDataCache().getAuthToken(),
                    gameID
            ));
        } catch (ResponseException ignored) {
            // 连线已经没了也照样回大厅
        }

        closingLocally = true;
        gameplaySocket.close();
        client.getDataCache().setCurrentGameID(0);
        throw new UIStateException(new PostloginUI(client), "Left '" + gameName + "'. Back in the lobby.");
    }

    private ChessPosition parsePosition(String token) throws ResponseException {
        String normalized = token.toLowerCase();
        if (!normalized.matches("[a-h][1-8]"))
            throw new ResponseException("Use chess coordinates like e2 or h7.", 400);

        int column = normalized.charAt(0) - 'a' + 1;
        int row = normalized.charAt(1) - '0';
        return new ChessPosition(row, column);
    }

    private ChessPiece.PieceType parsePromotion(String token) throws ResponseException {
        return switch (token.toLowerCase()) {
            case "q", "queen" -> ChessPiece.PieceType.QUEEN;
            case "r", "rook" -> ChessPiece.PieceType.ROOK;
            case "b", "bishop" -> ChessPiece.PieceType.BISHOP;
            case "n", "knight" -> ChessPiece.PieceType.KNIGHT;
            default -> throw new ResponseException("Promotion must be queen, rook, bishop, or knight.", 400);
        };
    }

    private String formatPosition(ChessPosition position) {
        char file = (char) ('a' + position.getColumn() - 1);
        return file + Integer.toString(position.getRow());
    }

    private String footerText() {
        String roleText = isPlayer
                ? "You are playing " + perspective.name().toLowerCase() + "."
                : "You are observing this game.";
        return "Game: " + gameName + "\n" + roleText + "\nType 'help' for gameplay commands.";
    }

    private void printAsync(String output) {
        if (output == null || output.isBlank())
            return;

        synchronized (CONSOLE_LOCK) {
            System.out.println();
            System.out.println(output);
            System.out.print("> ");
        }
    }

    @Override
    public void onLoadGame(LoadGameMessage message) {
        drawer.setChessGame(message.getGame());
        printAsync(renderBoard());
    }

    @Override
    public void onNotification(NotificationMessage message) {
        printAsync(EscapeSequences.SET_TEXT_COLOR_BLUE + message.getMessage() + EscapeSequences.RESET_TEXT_COLOR);
    }

    @Override
    public void onError(ErrorMessage message) {
        printAsync(formatError(message.getErrorMessage()));
    }

    @Override
    public void onClose(String reason) {
        if (closingLocally)
            return;
        printAsync(formatError(reason));
    }

    @Override
    public String displayHelpInfo() {
        if (isPlayer) {
            return """
                    --- GAME COMMANDS ---
                    - redraw                         | Draw the latest board again.
                    - move <from> <to> [promotion]  | Make a move, for example: move e2 e4
                    - highlight <square>            | Highlight legal moves for a piece.
                    - resign                        | Ask to resign the game.
                    - leave                         | Leave the game and return to the lobby.
                    - help                          | Display this help menu.
                    """;
        }

        return """
                --- OBSERVER COMMANDS ---
                - redraw              | Draw the latest board again.
                - highlight <square>  | Highlight legal moves for a piece.
                - leave               | Leave the game and return to the lobby.
                - help                | Display this help menu.
                """;
    }
}