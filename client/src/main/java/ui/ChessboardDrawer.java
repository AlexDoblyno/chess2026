package ui;

import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

public class ChessboardDrawer {
    private ChessGame chessGame;
    private ChessGame.TeamColor perspective;

    public ChessboardDrawer() {
        chessGame = new ChessGame();
        perspective = ChessGame.TeamColor.WHITE;
    }

    public ChessboardDrawer(ChessGame currentGame, ChessGame.TeamColor teamColor) {
        chessGame = currentGame;
        perspective = teamColor;
    }

    public String drawBoardString() {
        // 🚨 修复连打 5 次的 Bug：把 StringBuilder 变成局部变量，每次清空重来
        StringBuilder boardString = new StringBuilder();

        String formatCoordinates = EscapeSequences.SET_TEXT_BOLD + EscapeSequences.SET_BG_COLOR_DARK_GREEN + EscapeSequences.SET_TEXT_COLOR_WHITE;
        String clearFormatting = EscapeSequences.RESET_TEXT_BOLD_FAINT + EscapeSequences.RESET_TEXT_COLOR + EscapeSequences.RESET_BG_COLOR;

        // 判断当前视角是否为白方（如果观战 OBSERVE 传入 null，默认用白方视角）
        boolean isWhite = (perspective != ChessGame.TeamColor.BLACK);

        boardString.append(EscapeSequences.ERASE_SCREEN);

        // 🚨 修复字母行翻转：白方 a->h，黑方 h->a
        String letters = isWhite ?
                " \u2003 a  \u2003 b  \u2003 c  \u2003 d  \u2003 e  \u2003 f  \u2003 g  \u2003 h  \u2003 " :
                " \u2003 h  \u2003 g  \u2003 f  \u2003 e  \u2003 d  \u2003 c  \u2003 b  \u2003 a  \u2003 ";

        // 打印顶部字母标签
        boardString.append(formatCoordinates).append(letters).append(clearFormatting).append("\n");

        // Loop 打印棋盘核心逻辑
        for (int rowOffset = 0; rowOffset < 8; rowOffset++) {
            // 🚨 修复数字列翻转：白方从上到下是 8->1，黑方从上到下是 1->8
            int displayRow = isWhite ? (8 - rowOffset) : (1 + rowOffset);

            // 打印左侧数字标签
            boardString.append(formatCoordinates).append(" ").append(displayRow).append(" ").append(clearFormatting);

            for (int colOffset = 0; colOffset < 8; colOffset++) {
                // 列坐标也随着视角翻转：白方 1->8，黑方 8->1
                int displayCol = isWhite ? (1 + colOffset) : (8 - colOffset);

                ChessPosition printPosition = new ChessPosition(displayRow, displayCol);
                ChessPiece printPiece = getChessGame().getBoard().getPiece(printPosition);

                // 🚨 绝对正确的棋盘颜色算法：坐标 (row + col) 为奇数是白格，偶数是黑格（例如 a1 = 1+1 = 2，黑格）
                boolean isLightSquare = (displayRow + displayCol) % 2 != 0;
                String bgColor = isLightSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;

                boardString.append(bgColor).append(getPiece(printPiece));
            }

            // 打印右侧数字标签
            boardString.append(formatCoordinates).append(" ").append(displayRow).append(" ").append(clearFormatting).append("\n");
        }

        // 打印底部字母标签
        boardString.append(formatCoordinates).append(letters).append(clearFormatting).append("\n");

        return boardString.toString();
    }

    /**
     * Function to get the correct piece for display on the board
     *
     * @param chessPiece is the piece we're checking
     * @return the correct color and Unicode chess piece
     */
    private String getPiece(ChessPiece chessPiece) {
        StringBuilder pieceString = new StringBuilder();
        if (chessPiece == null) {
            pieceString.append("  \u2003  ");
        } else {
            pieceString.append(" ");
            switch (chessPiece.getPieceType()) {
                case KING -> pieceString.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING);
                case QUEEN -> pieceString.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN);
                case BISHOP -> pieceString.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP);
                case KNIGHT -> pieceString.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT);
                case ROOK -> pieceString.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK);
                case PAWN -> pieceString.append(chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                        EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN);
            }
            pieceString.append(" ");
        }
        return pieceString.toString();
    }

    public ChessGame getChessGame() {
        return chessGame;
    }

    public void setChessGame(ChessGame chessGame) {
        this.chessGame = chessGame;
    }

    public ChessGame.TeamColor getPerspective() {
        return perspective;
    }

    public void setPerspective(ChessGame.TeamColor perspective) {
        this.perspective = perspective;
    }
}