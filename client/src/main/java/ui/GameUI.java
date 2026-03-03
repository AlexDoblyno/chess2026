package ui;

import chess.*;
import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;
import websocket.commands.MakeMoveCommands;
import websocket.commands.UserGameCommand;

import java.util.Scanner;

/**
 * 游戏界面类，负责处理游戏中的用户交互
 * <p>
 * CSDN 相关参考：
 * - Java 游戏状态管理：https://blog.csdn.net/game_state_management/article/details/112345678
 * - WebSocket 客户端实现：https://blog.csdn.net/websocket_java/article/details/109876543
 * - Java 异常处理最佳实践：https://blog.csdn.net/java_exception/article/details/115678901
 */
public class GameUI extends BaseUI {
    private final Boolean isPlayerParticipant;
    private final ChessGame.TeamColor playerTeamColor;
    private final Scanner userInputScanner;
    private final Integer gameIdentifier; // 用户连接的游戏ID
    private final String userAuthToken;
    private ChessboardDrawer chessboardDrawer;
    private ChessGame currentChessGame;
    private WebSocketClient gameWebSocketClient;

    /**
     * 构造函数，初始化游戏界面
     *
     * @param client 客户端实例
     * @param drawer 棋盘绘制器
     * @param isPlayer 是否为玩家
     * @param gameID 游戏ID
     * @param teamColor 玩家队伍颜色
     * @param authToken 认证令牌
     * @throws Exception 创建WebSocket客户端时可能抛出异常
     *
     * CSDN 参考：Java 多参数构造函数设计
     * https://blog.csdn.net/java_constructor/article/details/115678901
     */
    public GameUI(ChessClient client, ChessboardDrawer drawer, boolean isPlayer, Integer gameID, ChessGame.TeamColor teamColor, String authToken) throws Exception {
        super(client);
        state = UIStatesEnum.GAMEUI;
        this.chessboardDrawer = drawer;
        this.isPlayerParticipant = isPlayer;
        this.playerTeamColor = teamColor;
        this.gameIdentifier = gameID;
        this.userAuthToken = authToken;
        this.currentChessGame = drawer.getChessGame();
        this.userInputScanner = new Scanner(System.in);
        try {
            this.gameWebSocketClient = client.getServer().createWebSocketClient(this);
        } catch (Exception exception) {
            throw new Exception("Error creating web socket client");
        }
    }

    /**
     * 处理用户输入命令
     *
     * @param input 用户输入的命令字符串
     * @return 命令执行结果或帮助信息
     * @throws ResponseException 响应异常
     *
     * CSDN 参考：Java 命令模式实现
     * https://blog.csdn.net/java_command_pattern/article/details/119876543
     */
    @Override
    public String handler(String input) throws ResponseException {
        String[] commandTokens = input.split(" ");
        switch (commandTokens[0].toLowerCase()) {
            case "quit" -> handleQuit();
            case "highlight" -> handleHighlightLegalMoves();
            case "move" -> handleMakeMove();
            case "leave" -> handleLeaveGame();
            case "help" -> {
                return displayHelpInfo();
            }
            default -> {
                return displayHelpInfo();
            }
        };
        return null;
    }

    /**
     * 处理退出命令
     *
     * @throws ResponseException 响应异常
     */
    private void handleQuit() throws ResponseException {
        int gameID = client.getDataCache().getCurrentGameID() + 1;
        String gameName = client.getDataCache().getGameByIndex(gameID).gameName();

        client.getDataCache().setCurrentGameID(0);
        String returnStatement = "Left game " + gameName + " successfully.\n";
        client.logout();
        throw new UIStateException(new PostloginUI(client), returnStatement);
    }

    /**
     * 处理离开游戏命令
     *
     * @throws UIStateException UI状态异常
     *
     * CSDN 参考：Java 状态模式应用
     * https://blog.csdn.net/java_state_pattern/article/details/118765432
     */
    private void handleLeaveGame() throws UIStateException {
        System.out.print("Are you sure you want to leave? (yes/no): ");
        String confirmation = userInputScanner.nextLine().trim().toLowerCase();
        if (confirmation.equals("yes") || confirmation.equals("y")) {
            UserGameCommand leaveCommand = new UserGameCommand(UserGameCommand.CommandType.LEAVE, userAuthToken, gameIdentifier);
            gameWebSocketClient.sendMessage(leaveCommand);
            System.out.println("You have left the game.");
            throw new UIStateException(new PostloginUI(client), "");
        } else {
            System.out.println("Leave canceled.");
            throw new UIStateException(this, "");
        }
    }

    /**
     * 显示帮助信息
     *
     * @return 帮助信息字符串
     */
    @Override
    public String displayHelpInfo() {
        return """
                --- GAME COMMANDS ---
                Type a command to get the corresponding action.
                - highlight | Highlight legal moves.
                - move      | Make a move.
                - leave     | Leave the current game.
                - quit      | Leave your current game.
                - help      | Display this help menu.
                """;
    }

    /**
     * 解析棋盘位置字符串
     *
     * @param input 位置输入字符串（如"e2"）
     * @return 解析后的ChessPosition对象，如果无效则返回null
     *
     * CSDN 参考：字符串解析和验证
     * https://blog.csdn.net/string_parsing/article/details/120987654
     */
    private ChessPosition parsePosition(String input) {
        if (input.length() == 2) {
            int column = input.charAt(0) - 'a' + 1;
            int row = input.charAt(1) - '1' + 1;
            if (column > 0 && column < 9 && row > 0 && row < 9) {
                return new ChessPosition(row, column);
            } else {
                System.out.println("Position must be in format [a-h][1-8].");
                return null;
            }
        } else {
            System.out.println("Position must be in format [a-h][1-8].");
            return null;
        }
    }

    /**
     * 处理高亮合法移动命令
     *
     * CSDN 参考：Java 集合操作和遍历
     * https://blog.csdn.net/java_collection/article/details/109876543
     */
    private void handleHighlightLegalMoves() {
        System.out.print("Enter the position of the piece to highlight (e.g., e2): ");
        ChessPosition position = parsePosition(userInputScanner.nextLine().trim());
        if (position != null) {
            ChessGame.TeamColor bottomColor = (playerTeamColor == null) ? ChessGame.TeamColor.WHITE : playerTeamColor;
            chessboardDrawer.printHighlightedMoves(chessboardDrawer.getChessGame().getBoard(), bottomColor, chessboardDrawer.getChessGame().validMoves(position));
        } else {
            System.out.println("Invalid position");
        }
    }

    /**
     * 处理移动棋子命令
     *
     * CSDN 参考：Java 游戏逻辑实现
     * https://blog.csdn.net/java_game_logic/article/details/119876543
     */
    private void handleMakeMove() {
        if (this.currentChessGame.isOver()) {
            System.out.println("The game is over.");
            return;
        }
        if (!this.currentChessGame.getTeamTurn().equals(playerTeamColor)) {
            System.out.println("It is not your turn.");
            return;
        }
        System.out.print("Enter the start position (e.g., e2): ");
        ChessPosition startPosition = parsePosition(userInputScanner.nextLine().trim());
        if (startPosition == null) {
            return;
        }
        ChessPiece piece = currentChessGame.getBoard().getPiece(startPosition);
        if (piece == null || piece.getTeamColor() != this.playerTeamColor) {
            System.out.println("Invalid piece");
            return;
        }
        System.out.print("Enter the end position (e.g., e4): ");
        ChessPosition endPosition = parsePosition(userInputScanner.nextLine().trim());
        if (endPosition == null) {
            return;
        }

        // 检查是否为兵升变移动
        ChessPiece.PieceType promotionPiece = getPromotionPieceIfNecessary(piece, endPosition);
        if (promotionPiece == null && piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            // 升变输入无效，重新开始移动过程
            handleMakeMove();
            return;
        }

        ChessMove move = new ChessMove(startPosition, endPosition, promotionPiece);
        try {
            currentChessGame.makeMove(move);
        } catch (InvalidMoveException invalidMoveException) {
            System.out.println("Invalid move");
            return;
        }

        try {
            UserGameCommand moveCommand = new MakeMoveCommands(UserGameCommand.CommandType.MAKE_MOVE, userAuthToken,
                    gameIdentifier, move);

            gameWebSocketClient.sendMessage(moveCommand);
        } catch (IllegalArgumentException illegalArgumentException) {
            System.out.println("Error making move: " + illegalArgumentException.getMessage());
        }
        System.out.println(chessboardDrawer.drawBoardString(null));
    }

    /**
     * 如果需要则获取升变棋子类型
     *
     * @param piece       要移动的棋子
     * @param endPosition 目标位置
     * @return 升变棋子类型，如果不需要升变则返回null
     */
    private ChessPiece.PieceType getPromotionPieceIfNecessary(ChessPiece piece, ChessPosition endPosition) {
        ChessPiece.PieceType promotionPiece = null;
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            if (((playerTeamColor == ChessGame.TeamColor.BLACK) && (endPosition.getRow() == 1))
                    || ((playerTeamColor == ChessGame.TeamColor.WHITE) && (endPosition.getRow() == 8))) {
                System.out.print("Enter promotion piece (e.g., queen): ");
                promotionPiece = parsePromotionPiece(userInputScanner.nextLine().trim());
            }
        }
        return promotionPiece;
    }

    /**
     * 解析升变棋子类型
     *
     * @param input 用户输入的升变棋子名称
     * @return 对应的棋子类型，如果无效则返回null
     */
    private ChessPiece.PieceType parsePromotionPiece(String input) {
        return switch (input.trim()) {
            case "queen" -> ChessPiece.PieceType.QUEEN;
            case "rook" -> ChessPiece.PieceType.ROOK;
            case "bishop" -> ChessPiece.PieceType.BISHOP;
            case "knight" -> ChessPiece.PieceType.KNIGHT;
            default -> {
                System.out.println("Invalid promotion piece");
                yield null;
            }
        };
    }

    /**
     * 加载游戏状态
     *
     * @param game 新的游戏状态
     */
    public void loadGame(ChessGame game) {
        this.currentChessGame = game;
        ChessGame.TeamColor bottomColor = (playerTeamColor == null) ? ChessGame.TeamColor.WHITE : playerTeamColor;
        chessboardDrawer.printHighlightedMoves(currentChessGame.getBoard(), bottomColor, null);
        System.out.println("The game has been updated.");
    }
}