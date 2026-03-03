package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class KingMoves extends PieceMoves {
    public KingMoves(ChessBoard GameBoard, ChessPosition startPosition) {
        super(GameBoard, startPosition);
        calculateMoves(GameBoard);
    }

    @Override
    public void calculateMoves(ChessBoard GameBoard) {
        ChessPosition checkPosition;
        for (int row = startPosition.getRow() - 1; row <= startPosition.getRow() + 1; row++) {
            for (int col = startPosition.getColumn() - 1; col <= startPosition.getColumn() + 1; col++) {
                if(isInBounds(row, col)) {
                    checkPosition = new ChessPosition(row, col);
                    checkSpace(GameBoard, checkPosition);
                }
            }
        }
    }
}
