package ui;

import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

public class ChessboardDrawer {
    private ChessGame chessGame;
    private ChessGame.TeamColor perspective;
    private final StringBuilder boardString;

    public ChessboardDrawer() {
        chessGame = new ChessGame();
        perspective = ChessGame.TeamColor.WHITE;
        boardString = new StringBuilder();
    }

    public ChessboardDrawer(ChessGame currentGame, ChessGame.TeamColor teamColor) {
        chessGame = currentGame;
        perspective = teamColor;
        boardString = new StringBuilder();
    }

    public String drawBoardString() {

        String formatCoordinates = EscapeSequences.SET_TEXT_BOLD + EscapeSequences.SET_BG_COLOR_DARK_GREEN;
        String clearFormatting = EscapeSequences.RESET_TEXT_BOLD_FAINT + EscapeSequences.RESET_TEXT_COLOR;

        // 通过当前队伍设置打印方向
        int direction = (perspective == ChessGame.TeamColor.WHITE) ? 8 : (0);

        boardString.append(EscapeSequences.ERASE_SCREEN);

        // 打印A~H标签
        boardString.append(formatCoordinates);
        boardString.append("   a b c d e f g h\n");
        boardString.append(clearFormatting);

        writeChessBoard(direction, formatCoordinates, clearFormatting);
        return boardString.toString();
    }

    private void writeChessBoard(int direction, String formatCoordinates, String clearFormatting) {
        // Loop打印
        for (int row = 0; row < 8; row++) {
            // 打印1~8标签
            int displayRow = direction - row;
            boardString.append(formatCoordinates).append(displayRow).append(" ").append(clearFormatting);

            // 打印期盼
            for (int col = 0; col < 8; col++) {
                ChessPosition printPosition = new ChessPosition(row, col);
                ChessPiece printPiece = getChessGame().getBoard().getPiece(printPosition);
                boardString.append(getSquareColor(row, col)).append(getPiece(printPiece));
                boardString.append(EscapeSequences.RESET_TEXT_COLOR).append(EscapeSequences.RESET_BG_COLOR);
            }
            boardString.append("\n");
        }
    }

    private String getSquareColor(int row, int col) {
        boolean isWhiteSquare = (row + col) % 2 == 0;
        return (isWhiteSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY);
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
            pieceString.append("   ");
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
            ;
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
