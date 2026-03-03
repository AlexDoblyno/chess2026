package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class QueenMoves extends PieceMovesFar {

    public QueenMoves(ChessBoard GameBoard, ChessPosition startPosition) {
        super(GameBoard, startPosition);
        calculateMoves(GameBoard);
    }

    @Override
    public void calculateMoves(ChessBoard GameBoard) {
        checkLine(GameBoard,-1, -1);
        checkLine(GameBoard,1, 1);
        checkLine(GameBoard,1, -1);
        checkLine(GameBoard,-1, 1);
        checkLine(GameBoard,-1, 0);
        checkLine(GameBoard,1, 0);
        checkLine(GameBoard,0, -1);
        checkLine(GameBoard,0, 1);
    }
}
