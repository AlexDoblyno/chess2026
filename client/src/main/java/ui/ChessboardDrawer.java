package ui;

import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
        return drawBoardString(null, null);
    }

    public String drawBoardString(Collection<ChessPosition> highlightedSquares, ChessPosition selectedSquare) {
        StringBuilder boardString = new StringBuilder();
        Set<ChessPosition> highlights = highlightedSquares == null ? Set.of() : new HashSet<>(highlightedSquares);

        String formatCoordinates = EscapeSequences.SET_TEXT_BOLD + EscapeSequences.SET_BG_COLOR_DARK_GREEN + EscapeSequences.SET_TEXT_COLOR_WHITE;
        String clearFormatting = EscapeSequences.RESET_TEXT_BOLD_FAINT + EscapeSequences.RESET_TEXT_COLOR + EscapeSequences.RESET_BG_COLOR;

        boolean isWhite = (perspective != ChessGame.TeamColor.BLACK);

        boardString.append(EscapeSequences.ERASE_SCREEN);

        String letters = isWhite ?
                " \u2003 a  \u2003 b  \u2003 c  \u2003 d  \u2003 e  \u2003 f  \u2003 g  \u2003 h  \u2003 " :
                " \u2003 h  \u2003 g  \u2003 f  \u2003 e  \u2003 d  \u2003 c  \u2003 b  \u2003 a  \u2003 ";

        boardString.append(formatCoordinates).append(letters).append(clearFormatting).append("\n");

        for (int rowOffset = 0; rowOffset < 8; rowOffset++) {
            int displayRow = isWhite ? (8 - rowOffset) : (1 + rowOffset);

            boardString.append(formatCoordinates).append(" ").append(displayRow).append(" ").append(clearFormatting);

            for (int colOffset = 0; colOffset < 8; colOffset++) {
                int displayCol = isWhite ? (1 + colOffset) : (8 - colOffset);

                ChessPosition printPosition = new ChessPosition(displayRow, displayCol);
                ChessPiece printPiece = getChessGame().getBoard().getPiece(printPosition);

                String bgColor = squareBackground(displayRow, displayCol, printPosition, highlights, selectedSquare);

                boardString.append(bgColor).append(getPiece(printPiece));
            }

            boardString.append(formatCoordinates).append(" ").append(displayRow).append(" ").append(clearFormatting).append("\n");
        }

        boardString.append(formatCoordinates).append(letters).append(clearFormatting).append("\n");

        return boardString.toString();
    }

    private String squareBackground(int displayRow, int displayCol, ChessPosition printPosition,
                                    Set<ChessPosition> highlights, ChessPosition selectedSquare) {
        if (selectedSquare != null && selectedSquare.equals(printPosition))
            return EscapeSequences.SET_BG_COLOR_YELLOW;
        if (highlights.contains(printPosition))
            return EscapeSequences.SET_BG_COLOR_GREEN;

        boolean isLightSquare = (displayRow + displayCol) % 2 != 0;
        return isLightSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;
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