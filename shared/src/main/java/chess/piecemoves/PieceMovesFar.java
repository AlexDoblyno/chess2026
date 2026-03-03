package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public abstract class PieceMovesFar extends PieceMoves {
    public PieceMovesFar(ChessBoard GameBoard, ChessPosition startPosition) {
        super(GameBoard, startPosition);
    }

    /**
     * Checks any straight line. rowDirection and colDirection determine which direction to check.
     * @param rowDirection -1 for down, 0 for same row, 1 for up
     * @param colDirection -1 for left, 0 for same col, 1 for right
     */
    protected void checkLine(ChessBoard GameBoard, int rowDirection, int colDirection) {
        boolean keepChecking = true;
        int row = startPosition.getRow() + rowDirection;
        int col = startPosition.getColumn() + colDirection;
        ChessPosition checkPosition;
        while (keepChecking) {
            if (rowDirection != 0 || colDirection != 0) {
                if (isInBounds(row, col)) {
                    keepChecking = checkSpace(GameBoard, new ChessPosition(row, col));
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
