package chess.piecemoves;

import chess.*;

public class PawnMoves extends PieceMoves {
    private int direction;      // -1 for down, 1 for up
    private int endzone;

    public PawnMoves(ChessBoard gameBoard, ChessPosition startPosition) {
        super(gameBoard, startPosition);
        setDirection();
        calculateMoves();
    }

    @Override
    public void calculateMoves() {
        checkCorners();
        checkFront();
    }

    private void setDirection() {
        if (Team == ChessGame.TeamColor.BLACK) {
            direction = -1;
            endzone = 1;
        }
        else {
            direction = 1;
            endzone = 8;
        }
    }

    private boolean isFirstMove() {
        if (Team == ChessGame.TeamColor.BLACK && StartPosition.getRow() == 7) {
            return true;
        }
        else return Team == ChessGame.TeamColor.WHITE && StartPosition.getRow() == 2;
    }

    private void checkCorners() {
        int row;
        ChessPiece targetedPiece;
        ChessPosition checkPosition;
        for (int i = StartPosition.getColumn() - 1; i <= StartPosition.getColumn() + 1; i+=2) {
            row = StartPosition.getRow() + direction;
            if (isInBounds(row, i)) {
                checkPosition = new ChessPosition(row, i);
                targetedPiece = GameBoard.getPiece(checkPosition);
                if (targetedPiece != null && targetedPiece.getTeamColor() != Team) {
                    addMove(checkPosition);
                }
            }
        }
    }

    private void checkFront() {
        ChessPosition checkPosition = new ChessPosition(StartPosition.getRow() + direction, StartPosition.getColumn());
        if(GameBoard.getPiece(checkPosition) == null) {
            addMove(checkPosition);
            if (isFirstMove()) {
                checkPosition = new ChessPosition(checkPosition.getRow() + direction, checkPosition.getColumn());
                if(GameBoard.getPiece(checkPosition) == null) {
                    addMove(checkPosition);
                }
            }
        }
    }

    private void addMove(ChessPosition EndPosition) {
        if (EndPosition.getRow() == endzone) {
            promote(EndPosition);
        }
        else {
            MoveList.add(new ChessMove(StartPosition, EndPosition));
        }
    }

    private void promote (ChessPosition EndPosition) {
        MoveList.add(new ChessMove(StartPosition, EndPosition, ChessPiece.PieceType.ROOK));
        MoveList.add(new ChessMove(StartPosition, EndPosition, ChessPiece.PieceType.KNIGHT));
        MoveList.add(new ChessMove(StartPosition, EndPosition, ChessPiece.PieceType.BISHOP));
        MoveList.add(new ChessMove(StartPosition, EndPosition, ChessPiece.PieceType.QUEEN));
    }
}
