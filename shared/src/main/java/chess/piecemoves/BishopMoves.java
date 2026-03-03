package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class BishopMoves extends PieceMovesFar {
    public BishopMoves(ChessBoard GameBoard, ChessPosition startPosition) {
        super(GameBoard, startPosition);
        calculateMoves(GameBoard);
    }

    @Override
    public void calculateMoves(ChessBoard GameBoard) {
        checkLine(GameBoard,-1, -1);
        checkLine(GameBoard,1, 1);
        checkLine(GameBoard,1, -1);
        checkLine(GameBoard,-1, 1);
    }
}
