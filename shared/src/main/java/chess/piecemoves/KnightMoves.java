package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;

public class KnightMoves extends PieceMoves {
    public KnightMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        ChessPosition checkPosition;
        for (int row = StartPosition.getRow() - 2; row <= StartPosition.getRow() + 2; row++) {
            for (int col = StartPosition.getColumn() - 2; col <= StartPosition.getColumn() + 2; col++) {
                if (Math.abs(StartPosition.getRow() - row) + Math.abs(StartPosition.getColumn() - col) == 3 && isInBounds(row, col)) {
                    checkPosition = new ChessPosition(row, col);
                    checkSpace(checkPosition);
                }
            }
        }
    }
}
