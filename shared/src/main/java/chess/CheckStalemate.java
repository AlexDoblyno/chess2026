package chess;

import java.util.Collection;
import java.util.HashSet;

public class CheckStalemate {

    private ChessBoard GameBoard;

    CheckStalemate (ChessBoard GameBoard) {
        this.GameBoard = GameBoard;
    }

    public boolean isInStalemate(boolean isInCheck, Collection<ChessMove> teamMoves) {
        if (!isInCheck) {
            if (teamMoves.isEmpty()) {
                return true;
            }
            return GarbageBoardState();
        }
        return false;
    }
    private boolean GarbageBoardState() {
        if (containsNoLimitMovers()) {
            return false;
        }
        else {
            Collection<ChessPosition> whitePieces = getTeamPieces(ChessGame.TeamColor.WHITE);
            Collection<ChessPosition> blackPieces = getTeamPieces(ChessGame.TeamColor.BLACK);

            // Calculate total pieces
            int totalPieces = whitePieces.size() + blackPieces.size();
            if (totalPieces == 2) {
                return true;
            }
            else {
                return pieceExtraction(whitePieces, blackPieces, totalPieces);
            }
        }
    }

    private boolean pieceExtraction(Collection<ChessPosition> whitePieces, Collection<ChessPosition> blackPieces, int totalPieces) {
        Collection<ChessPosition> WHITEBishops = extractPieces(whitePieces, ChessPiece.PieceType.BISHOP);
        Collection<ChessPosition> BLACKBishops = extractPieces(blackPieces, ChessPiece.PieceType.BISHOP);
        whitePieces.removeAll(WHITEBishops);
        blackPieces.removeAll(BLACKBishops);

        Collection<ChessPosition> whiteKnights = extractPieces(whitePieces, ChessPiece.PieceType.KNIGHT);
        Collection<ChessPosition> blackKnights = extractPieces(blackPieces, ChessPiece.PieceType.KNIGHT);
        whitePieces.removeAll(whiteKnights);
        blackPieces.removeAll(blackKnights);
        // The remaining piece in whitePieces and blackPieces is the king.

        // board state: King vs King and Bishop OR King vs King and Knight
        if (totalPieces == 3 && (WHITEBishops.size() + BLACKBishops.size() +
                whiteKnights.size() + blackKnights.size() == 1)) {
            return true;
        }

        // board state: King vs King and all bishops are on the same color
        else if (whiteKnights.isEmpty() && blackKnights.isEmpty()){
            boolean BLACKBishopsSameColor = onSameColor(BLACKBishops);
            boolean WHITEBishopsSameColor = onSameColor(WHITEBishops);
            if (WHITEBishops.isEmpty() && !BLACKBishops.isEmpty() && onSameColor(BLACKBishops)) {
                return true;
            }
            else if (BLACKBishops.isEmpty() && !WHITEBishops.isEmpty() &&onSameColor(WHITEBishops)) {
                return true;
            }
            else {
                return !BLACKBishops.isEmpty() && onSameColor(WHITEBishops) && onSameColor(BLACKBishops);
            }
        }

        // board state: King and two knights vs king
        else {
            return (whiteKnights.size() == 2 && WHITEBishops.isEmpty() && blackKnights.isEmpty() && BLACKBishops.isEmpty()) ||
                    (blackKnights.size() == 2 && BLACKBishops.isEmpty() && whiteKnights.isEmpty() && WHITEBishops.isEmpty());
        }
    }
    private boolean onSameColor(Collection<ChessPosition> pieces) {
        int blackSquare = 0;
        int whiteSquare = 0;
        for (ChessPosition piece : pieces) {
            if (piece.getColumn()%2 == 0) {
                if (piece.getRow()%2 == 0) {
                    whiteSquare++;
                }
                else {
                    blackSquare++;
                }
            }
            else {
                if (piece.getRow()%2 == 0) {
                    blackSquare++;
                }
                else {
                    whiteSquare++;
                }
            }
        }
        return whiteSquare == 0 || blackSquare == 0;
    }
    private boolean containsNoLimitMovers() {
        ChessPosition checkPosition;
        ChessPiece checkPiece;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                checkPosition = new ChessPosition(row, col);
                checkPiece = GameBoard.getPiece(checkPosition);
                if (checkPiece != null) {
                    if (checkPiece.getPieceType() == ChessPiece.PieceType.PAWN ||
                            checkPiece.getPieceType() == ChessPiece.PieceType.QUEEN ||
                            checkPiece.getPieceType() == ChessPiece.PieceType.ROOK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private Collection<ChessPosition> extractPieces(Collection<ChessPosition> allPositions, ChessPiece.PieceType type) {
        Collection<ChessPosition> pieces = new HashSet<>();
        for (ChessPosition space : allPositions) {
            if (GameBoard.getPiece(space).getPieceType() == type) {
                pieces.add(space);
            }
        }
        return pieces;
    }
    private Collection<ChessPosition> getTeamPieces(ChessGame.TeamColor color) {
        Collection<ChessPosition> allPositions = new HashSet<>();
        ChessPosition checkPosition;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                checkPosition = new ChessPosition(row, col);
                if (GameBoard.getPiece(checkPosition) != null &&
                        GameBoard.getPiece(checkPosition).getTeamColor() == color) {
                    allPositions.add(checkPosition);
                }
            }
        }
        return allPositions;
    }

    public void setGameBoard(ChessBoard GameBoard) {
        this.GameBoard = GameBoard;
    }
}