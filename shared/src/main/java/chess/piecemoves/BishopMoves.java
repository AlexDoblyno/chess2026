package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class BishopMoves extends PieceMovesFar {
    public BishopMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        checkLine(-1, -1);
        checkLine(1, 1);
        checkLine(1, -1);
        checkLine(-1, 1);
    }
}
