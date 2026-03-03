package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class RookMoves extends PieceMovesFar {
    public RookMoves(ChessBoard GameBoard, ChessPosition startPosition) {
        super(GameBoard, startPosition);
        calculateMoves(GameBoard);
    }

    @Override
    public void calculateMoves(ChessBoard GameBoard) {
        checkLine(GameBoard,-1, 0);
        checkLine(GameBoard,1, 0);
        checkLine(GameBoard,0, -1);
        checkLine(GameBoard,0, 1);
    }
}
