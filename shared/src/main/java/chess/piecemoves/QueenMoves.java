package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class QueenMoves extends PieceMovesFar {

    public QueenMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }