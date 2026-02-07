package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.HashSet;

public class RookMoves extends PieceMovesFar {
    public RookMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        checkLine(-1, 0);
        checkLine(1, 0);
        checkLine(0, -1);
        checkLine(0, 1);
    }
}
