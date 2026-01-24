package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessPosition;

public class BishopMoves extends PieceMovesFar {
    public BishopMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        super(GameBoard, StartPosition);
        calculateMoves();
    }
