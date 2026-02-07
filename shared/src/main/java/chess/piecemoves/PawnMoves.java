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
        if (team == ChessGame.TeamColor.BLACK) {
            direction = -1;
            endzone = 1;
        }
        else {
            direction = 1;
            endzone = 8;
        }
    }

    private boolean isFirstMove() {
        if (team == ChessGame.TeamColor.BLACK && startPosition.getRow() == 7) {
            return true;
        }
        else return team == ChessGame.TeamColor.WHITE && startPosition.getRow() == 2;
    }

    private void checkCorners() {
        int row;
        ChessPiece targetedPiece;
        ChessPosition checkPosition;
        for (int i = startPosition.getColumn() - 1; i <= startPosition.getColumn() + 1; i+=2) {
            row = startPosition.getRow() + direction;
            if (isInBounds(row, i)) {
                checkPosition = new ChessPosition(row, i);
                targetedPiece = gameBoard.getPiece(checkPosition);
                if (targetedPiece != null && targetedPiece.getTeamColor() != team) {
                    addMove(checkPosition);
                }
            }
        }
    }

    private void checkFront() {
        ChessPosition checkPosition = new ChessPosition(startPosition.getRow() + direction, startPosition.getColumn());
        if(gameBoard.getPiece(checkPosition) == null) {
            addMove(checkPosition);
            if (isFirstMove()) {
                checkPosition = new ChessPosition(checkPosition.getRow() + direction, checkPosition.getColumn());
                if(gameBoard.getPiece(checkPosition) == null) {
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
            moveList.add(new ChessMove(startPosition, EndPosition));
        }
    }

    private void promote (ChessPosition EndPosition) {
        moveList.add(new ChessMove(startPosition, EndPosition, ChessPiece.PieceType.ROOK));
        moveList.add(new ChessMove(startPosition, EndPosition, ChessPiece.PieceType.KNIGHT));
        moveList.add(new ChessMove(startPosition, EndPosition, ChessPiece.PieceType.BISHOP));
        moveList.add(new ChessMove(startPosition, EndPosition, ChessPiece.PieceType.QUEEN));
    }
}
