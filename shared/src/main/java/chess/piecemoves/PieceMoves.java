package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.HashSet;
import java.util.Objects;

public abstract class PieceMoves {

    protected ChessBoard gameBoard;
    protected ChessPosition startPosition;
    protected HashSet<ChessMove> moveList;
    protected ChessGame.TeamColor team;

    public PieceMoves(ChessBoard gameBoard, ChessPosition startPosition) {
        this.gameBoard = gameBoard;
        this.startPosition = startPosition;
        team = gameBoard.getPiece(startPosition).getTeamColor();
        moveList = new HashSet<ChessMove>();
    }

    public void calculateMoves(){}

    public HashSet<ChessMove> getMoveList() {
        return moveList;
    }

    protected boolean isInBounds(int row, int col) {
        return  row > 0 && col > 0 && row <= 8 && col <= 8;
    }

    protected boolean checkSpace(ChessPosition EndPosition) {
        if (gameBoard.getPiece(EndPosition) != null) {
            if (gameBoard.getPiece(EndPosition).getTeamColor() != team) {
                moveList.add(new ChessMove(startPosition, EndPosition));
                System.out.print("Captured piece at (" + EndPosition.getRow() + ", " + EndPosition.getColumn() + "): ");
            }
            System.out.println("Stop checking");
            return false;
        }
        else {
            moveList.add(new ChessMove(startPosition, EndPosition));
            System.out.println("Added move to null space at (" + EndPosition.getRow() + ", " + EndPosition.getColumn() + ") ");
            return true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PieceMoves that)) {
            return false;
        }
        return Objects.equals(moveList, that.moveList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(moveList);
    }
}
