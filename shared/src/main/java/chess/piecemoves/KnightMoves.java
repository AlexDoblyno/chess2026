package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class KnightMoves extends PieceMoves {
    public KnightMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        ChessPosition checkPosition;
        for (int row = startPosition.getRow() - 2; row <= startPosition.getRow() + 2; row++) {
            for (int col = startPosition.getColumn() - 2; col <= startPosition.getColumn() + 2; col++) {
                if (Math.abs(startPosition.getRow() - row) + Math.abs(startPosition.getColumn() - col) == 3 && isInBounds(row, col)) {
                    checkPosition = new ChessPosition(row, col);
                    checkSpace(checkPosition);
                }
            }
        }
    }
}
