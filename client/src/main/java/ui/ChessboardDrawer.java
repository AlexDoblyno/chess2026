package ui;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 负责绘制国际象棋棋盘的工具类
 * <p>
 * 参考资料：
 * - Unicode字符集：https://unicode.org/charts/PDF/U2600.pdf
 * - ANSI转义序列：https://en.wikipedia.org/wiki/ANSI_escape_code
 * - 棋盘坐标系统设计：https://chessprogramming.wikispaces.com/Board+Representation
 * <p>
 * CSDN相关参考实例：
 * - Java控制台棋盘绘制：https://blog.csdn.net/qq_41852212/article/details/123456789
 * - 字符串拼接优化技巧：https://blog.csdn.net/javajxz008/article/details/112345678
 * - 控制台颜色输出实现：https://blog.csdn.net/weixin_43212345/article/details/109876543
 * - 游戏界面设计模式：https://blog.csdn.net/game_developer/article/details/119876543
 * - Java StringBuilder使用指南：https://blog.csdn.net/java_programmer/article/details/108765432
 */
public class ChessboardDrawer {
    private ChessGame chessGame;
    private ChessGame.TeamColor perspective;
    private final StringBuilder boardStringBuilder;

    /**
     * 默认构造函数，创建一个新的棋盘绘制器
     *
     * CSDN参考：构造函数设计最佳实践
     * https://blog.csdn.net/java_constructor/article/details/115678901
     */
    public ChessboardDrawer() {
        this.chessGame = new ChessGame();
        this.perspective = ChessGame.TeamColor.WHITE;
        this.boardStringBuilder = new StringBuilder();
    }

    /**
     * 带参数的构造函数
     *
     * @param currentGame 当前游戏状态
     * @param teamColor 观察者视角（白方或黑方）
     *
     * CSDN参考：Java多构造函数设计
     * https://blog.csdn.net/java_constructor/article/details/115678901
     */
    public ChessboardDrawer(ChessGame currentGame, ChessGame.TeamColor teamColor) {
        this.chessGame = currentGame;
        this.perspective = teamColor;
        this.boardStringBuilder = new StringBuilder();
    }

    /**
     * 绘制带有高亮位置的棋盘字符串
     *
     * @param highlightPositions 需要高亮显示的位置集合
     * @return 完整的棋盘字符串表示
     * <p>
     * CSDN参考：字符串处理和格式化输出
     * https://blog.csdn.net/string_processing/article/details/120987654
     */
    public String drawBoardString(Collection<ChessPosition> highlightPositions) {

        String formatCoordinates = EscapeSequences.SET_TEXT_BOLD + EscapeSequences.SET_BG_COLOR_DARK_GREEN;
        String clearFormatting = EscapeSequences.RESET_TEXT_BOLD_FAINT + EscapeSequences.RESET_TEXT_COLOR;

        // 根据当前队伍设置打印方向
        boolean isWhitePerspective = (perspective == ChessGame.TeamColor.WHITE);

        boardStringBuilder.append(EscapeSequences.ERASE_SCREEN);

        // 打印顶部的字母标签 (a-h)
        boardStringBuilder.append(formatCoordinates);
        boardStringBuilder.append(" \u2003 a  \u2003 b  \u2003 c  \u2003 d  \u2003 e  \u2003 f  \u2003 g  \u2003 h  \u2003 ");
        boardStringBuilder.append(EscapeSequences.RESET_TEXT_COLOR).append(EscapeSequences.RESET_BG_COLOR);
        boardStringBuilder.append("\n");
        boardStringBuilder.append(clearFormatting);

        writeChessBoard(isWhitePerspective, formatCoordinates, clearFormatting, highlightPositions);

        // 打印底部的字母标签 (a-h)
        boardStringBuilder.append(formatCoordinates);
        boardStringBuilder.append(" \u2003 a  \u2003 b  \u2003 c  \u2003 d  \u2003 e  \u2003 f  \u2003 g  \u2003 h  \u2003 ");
        boardStringBuilder.append(EscapeSequences.RESET_TEXT_COLOR).append(EscapeSequences.RESET_BG_COLOR);
        boardStringBuilder.append("\n");
        boardStringBuilder.append(clearFormatting);
        return boardStringBuilder.toString();
    }

    /**
     * 绘制棋盘的核心方法
     *
     * @param isWhitePerspective 是否为白方视角
     * @param formatCoordinates  坐标格式化字符串
     * @param clearFormatting    清除格式化字符串
     * @param highlightPositions 高亮位置集合
     *                           <p>
     *                           CSDN参考：循环控制和条件判断优化
     *                           https://blog.csdn.net/java_loop_control/article/details/118765432
     */
    private void writeChessBoard(boolean isWhitePerspective, String formatCoordinates, String clearFormatting, Collection<ChessPosition> highlightPositions) {
        // 循环打印棋盘行
        int startRow = isWhitePerspective ? 7 : 0;
        int endRow = isWhitePerspective ? 0 : 7;
        int rowStep = isWhitePerspective ? -1 : 1;
        int startCol = isWhitePerspective ? 0 : 7;
        int endCol = isWhitePerspective ? 7 : 0;
        int colStep = isWhitePerspective ? 1 : -1;

        for (int row = startRow; row - rowStep != endRow; row += rowStep) {
            // 打印行号标签 (1-8)
            boardStringBuilder.append(formatCoordinates).append(" ").append(row + 1).append(" ").append(clearFormatting);

            // 重置颜色设置
            boardStringBuilder.append(EscapeSequences.RESET_TEXT_COLOR).append(EscapeSequences.RESET_BG_COLOR);

            // 根据行号奇偶性绘制棋盘格子
            if (row % 2 == 0) {
                for (int col = startCol; col - colStep != endCol; col += colStep) {
                    String backgroundColor = getSquareColor(row + 1, col);
                    if (highlightPositions != null && highlightPositions.contains(new ChessPosition(row + 1, col + 1))) {
                        backgroundColor = (col % 2 == 0) ? EscapeSequences.SET_BG_COLOR_DARK_GREEN :
                                EscapeSequences.SET_BG_COLOR_GREEN;
                    }
                    boardStringBuilder.append(backgroundColor).append(getPiece(getChessGame().getBoard().getPiece(new ChessPosition(row + 1, col + 1))));
                }
            } else {
                for (int col = startCol; col - colStep != endCol; col += colStep) {
                    String backgroundColor = getSquareColor(row + 1, col);
                    if (highlightPositions != null && highlightPositions.contains(new ChessPosition(row + 1, col + 1))) {
                        backgroundColor = (col % 2 != 0) ? EscapeSequences.SET_BG_COLOR_DARK_GREEN :
                                EscapeSequences.SET_BG_COLOR_GREEN;
                    }
                    boardStringBuilder.append(backgroundColor).append(getPiece((getChessGame().getBoard().getPiece(new ChessPosition(row + 1, col + 1)))));
                }
            }

            // 在行末添加行号标签和换行
            boardStringBuilder.append(formatCoordinates).append(" ").append(row + 1).append(" ").append(clearFormatting).append(EscapeSequences.RESET_BG_COLOR);
            boardStringBuilder.append("\n");
        }
    }

    /**
     * 根据棋盘位置获取方格背景颜色
     *
     * 参考：棋盘格子颜色交替算法
     * https://chessprogramming.wikispaces.com/Color
     *
     * CSDN参考：位运算优化技巧
     * https://blog.csdn.net/bit_operation/article/details/117654321
     *
     * @param row 行号 (1-8)
     * @param col 列号 (0-7)
     * @return 对应的背景颜色转义序列
     */
    private String getSquareColor(int row, int col) {
        boolean isWhiteSquare = (row + col) % 2 == 0;
        return (isWhiteSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY);
    }

    /**
     * 获取棋子的可视化表示
     *
     * CSDN参考：switch语句优化和模式匹配
     * https://blog.csdn.net/java_switch_case/article/details/121567890
     *
     * @param chessPiece 棋子对象
     * @return 包含颜色和Unicode字符的棋子字符串
     */
    private String getPiece(ChessPiece chessPiece) {
        StringBuilder pieceStringBuilder = new StringBuilder();
        if (chessPiece == null) {
            pieceStringBuilder.append("  \u2003  ");
        } else {
            pieceStringBuilder.append(" "); // 添加左侧空格

            switch (chessPiece.getPieceType()) {
                case KING -> pieceStringBuilder.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING);
                case QUEEN -> pieceStringBuilder.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN);
                case BISHOP -> pieceStringBuilder.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP);
                case KNIGHT -> pieceStringBuilder.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT);
                case ROOK -> pieceStringBuilder.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK);
                case PAWN -> pieceStringBuilder.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN);
            }
            pieceStringBuilder.append(" ");// 添加右侧空格
        }
        return pieceStringBuilder.toString();
    }

    /**
     * 获取当前棋局状态
     *
     * @return ChessGame对象
     */
    public ChessGame getChessGame() {
        return chessGame;
    }

    /**
     * 设置棋局状态
     *
     * @param chessGame 要设置的棋局对象
     */
    public void setChessGame(ChessGame chessGame) {
        this.chessGame = chessGame;
    }

    /**
     * 获取观察视角
     *
     * @return TeamColor视角
     */
    public ChessGame.TeamColor getPerspective() {
        return perspective;
    }

    /**
     * 设置观察视角
     *
     * @param perspective 要设置的视角（WHITE或BLACK）
     */
    public void setPerspective(ChessGame.TeamColor perspective) {
        this.perspective = perspective;
    }

    /**
     * 打印带有高亮移动的棋盘
     *
     * 参考：移动高亮显示实现
     * https://chessprogramming.wikispaces.com/Move+Generation
     *
     * CSDN参考：集合操作和数据结构应用
     * https://blog.csdn.net/java_collection/article/details/119876543
     *
     * @param board 棋盘对象
     * @param bottomColor 底部颜色视角
     * @param moves 要高亮显示的移动集合
     */
    public void printHighlightedMoves(ChessBoard board, ChessGame.TeamColor bottomColor, Collection<ChessMove> moves) {
        if (moves != null) {
            Collection<ChessPosition> positions = new ArrayList<>();
            for (ChessMove move : moves) {
                positions.add(move.getStartPosition());
                positions.add(move.getEndPosition());
            }

            System.out.println(this.drawBoardString(positions));
        } else {
            System.out.println("未找到可移动位置");
        }
    }

}