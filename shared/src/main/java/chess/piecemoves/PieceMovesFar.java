package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public abstract class PieceMovesFar extends PieceMoves {
    public PieceMovesFar(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
    }

    /**
     * Checks any straight line. rowDirection and colDirection determine which direction to check.
     * @param rowDirection -1 for down, 0 for same row, 1 for up
     * @param colDirection -1 for left, 0 for same col, 1 for right
     */
    protected void checkLine(int rowDirection, int colDirection) {
        boolean keepChecking = true;
        int row = StartPosition.getRow() + rowDirection;
        int col = StartPosition.getColumn() + colDirection;
        ChessPosition checkPosition;
        while (keepChecking) {
            if (rowDirection != 0 || colDirection != 0) {
                if (isInBounds(row, col)) {
                    keepChecking = checkSpace(new ChessPosition(row, col));
                    row += rowDirection;
                    col += colDirection;
                }
                else {
                    keepChecking = false;
                }
            }
            else {
                keepChecking = false;
            }
        }
    }
}
