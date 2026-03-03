package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class KingMoves extends PieceMoves {
    public KingMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        ChessPosition checkPosition;
        for (int row = StartPosition.getRow() - 1; row <= StartPosition.getRow() + 1; row++) {
            for (int col = StartPosition.getColumn() - 1; col <= StartPosition.getColumn() + 1; col++) {
                if(isInBounds(row, col)) {
                    checkPosition = new ChessPosition(row, col);
                    checkSpace(checkPosition);
                }
            }
        }
    }
}
