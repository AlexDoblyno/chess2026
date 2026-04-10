package chess.piecemoves;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.HashSet;
import java.util.Objects;

public abstract class PieceMoves {

    protected ChessBoard GameBoard;
    protected ChessPosition StartPosition;
    protected HashSet<ChessMove> MoveList;
    protected ChessGame.TeamColor Team;

    public PieceMoves(ChessBoard GameBoard, ChessPosition StartPosition) {
        this.GameBoard = GameBoard;
        this.StartPosition = StartPosition;
        Team = GameBoard.getPiece(StartPosition).getTeamColor();
        MoveList = new HashSet<>();
    }

    public void calculateMoves(){}

    public HashSet<ChessMove> getMoveList() {
        return MoveList;
    }

    protected boolean isInBounds(int row, int col) {
        return row > 0 && col > 0 && row <= 8 && col <= 8;
    }

    protected boolean checkSpace(ChessPosition EndPosition) {
        if (GameBoard.getPiece(EndPosition) != null) {
            if (GameBoard.getPiece(EndPosition).getTeamColor() != Team)
                MoveList.add(new ChessMove(StartPosition, EndPosition));
            return false;
        }

        MoveList.add(new ChessMove(StartPosition, EndPosition));
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PieceMoves that)) {
            return false;
        }
        return Objects.equals(MoveList, that.MoveList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(MoveList);
    }
}
