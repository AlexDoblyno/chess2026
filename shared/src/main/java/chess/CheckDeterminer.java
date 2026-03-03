package chess;

import java.util.Collection;

public class CheckDeterminer {
    private ChessBoard GameBoard;

    CheckDeterminer(ChessBoard GameBoard) {
        this.GameBoard = GameBoard;
    }
    public boolean isInCheck(ChessGame.TeamColor teamColor) {

        ChessPosition kingPosition = findKing(teamColor); //找到King
        if (kingPosition != null) {
            if (CheckKingStraights(kingPosition)) {
                return true;
            }
            return CheckKingKnights(kingPosition); //检查King四面八方有无敌人
        }
        return false;
    }
    private ChessPosition findKing(ChessGame.TeamColor teamColor) {
        ChessPosition checkPosition;
        ChessPiece checkPiece;
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                checkPosition = new ChessPosition(i, j);
                checkPiece = GameBoard.getPiece(checkPosition);
                if (checkPiece != null && checkPiece.getTeamColor() == teamColor
                        && checkPiece.getPieceType() == ChessPiece.PieceType.KING) {
                    return checkPosition;
                }
            }
        }
        return null;
    }

    private boolean CheckKingStraights(ChessPosition kingPosition) {
        for (int rowMod = -1; rowMod <= 1; rowMod++) {
            for (int colMod = -1; colMod <= 1; colMod++) {
                if (rowMod != 0 || colMod != 0) {
                    if (straightChecker(kingPosition, rowMod, colMod)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private boolean straightChecker(ChessPosition kingPosition, int rowMod, int colMod) {
        ChessPosition checkPosition;
        ChessPiece checkPiece;
        ChessGame.TeamColor kingColor = GameBoard.getPiece(kingPosition).getTeamColor();
        int row = kingPosition.getRow() + rowMod;
        int col = kingPosition.getColumn() + colMod;

        while (row <= 8 && col <= 8 && row >0 && col > 0) {
            checkPosition = new ChessPosition(row, col);
            if (GameBoard.getPiece(checkPosition) != null){
                checkPiece = GameBoard.getPiece(checkPosition);
                if (checkPiece.getTeamColor() != kingColor) {
                    return targetingKing(kingPosition, checkPiece, checkPosition);
                }
            }
            row += rowMod;
            col += colMod;
        }
        return false;
    }
    private boolean targetingKing(ChessPosition kingPosition, ChessPiece checkPiece, ChessPosition checkPosition) {
        // Create a movelist for the selected chess piece.
        Collection<ChessMove> targetMoves = checkPiece.pieceMoves(GameBoard, checkPosition);

        // Create two example moves that target the king's position. One for generic pieces, one for pawn promotion moves.
        ChessMove dangerMove = new ChessMove(checkPosition, kingPosition);
        ChessMove pawnDangerMove = new ChessMove(checkPosition, kingPosition, ChessPiece.PieceType.QUEEN);

        // If the danger move is in the move list, the king is in check.
        return targetMoves.contains(dangerMove) || targetMoves.contains(pawnDangerMove);
    }
    private boolean CheckKingKnights(ChessPosition kingPosition) {
        int kingRow = kingPosition.getRow();
        int kingCol = kingPosition.getColumn();
        ChessGame.TeamColor kingColor = GameBoard.getPiece(kingPosition).getTeamColor();
        ChessPosition checkPosition;

        for (int row = kingRow - 2; row <= kingRow + 2; row ++) {
            for (int col = kingCol - 2; col <= kingCol + 2; col++) {
                if (horseChecker(kingRow, row, kingCol, col, kingColor)) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean horseChecker(int kingRow, int row, int kingCol, int col, ChessGame.TeamColor kingColor) {
        ChessPosition checkPosition;
        if (Math.abs(kingRow - row) + Math.abs(kingCol - col) == 3 &&
                row <= 8 && col <= 8 && row >0 && col > 0) {
            checkPosition = new ChessPosition(row, col);
            ChessPiece checkPiece = GameBoard.getPiece(checkPosition);
            if (checkPiece != null && checkPiece.getPieceType() == ChessPiece.PieceType.KNIGHT
                    && checkPiece.getTeamColor() != kingColor) {
                return true;
            }
        }
        return false;
    }

    public void setGameBoard(ChessBoard GameBoard) {
        this.GameBoard = GameBoard;
    }
}
