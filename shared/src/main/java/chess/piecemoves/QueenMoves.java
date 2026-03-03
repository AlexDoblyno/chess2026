package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class QueenMoves extends PieceMovesFar {

    public QueenMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        checkLine(-1, -1);
        checkLine(1, 1);
        checkLine(1, -1);
        checkLine(-1, 1);
        checkLine(-1, 0);
        checkLine(1, 0);
        checkLine(0, -1);
        checkLine(0, 1);
    }
}
