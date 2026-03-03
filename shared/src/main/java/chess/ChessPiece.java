package chess;

import chess.piecemoves.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private ChessGame.TeamColor pieceColor;
    private PieceType type;
    private PieceMoves selectedPiece;
    private HashSet<PieceMoves> moveList;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch (board.getPiece(myPosition).getPieceType()) {
            case ROOK -> {
                selectedPiece = new RookMoves(board, myPosition);
                yield selectedPiece.getMoveList();
            }
            case BISHOP -> {
                selectedPiece = new BishopMoves(board, myPosition);
                yield selectedPiece.getMoveList();
            }
            case QUEEN -> {
                selectedPiece = new QueenMoves(board, myPosition);
                yield selectedPiece.getMoveList();
            }
            case KNIGHT -> {
                selectedPiece = new KnightMoves(board, myPosition);
                yield selectedPiece.getMoveList();
            }
            case KING -> {
                selectedPiece = new KingMoves(board, myPosition);
                yield selectedPiece.getMoveList();
            }
            case PAWN -> {
                selectedPiece = new PawnMoves(board, myPosition);
                yield selectedPiece.getMoveList();
            }
            case null, default -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChessPiece that)) {
            return false;
        }
        return pieceColor == that.pieceColor && type == that.type && Objects.equals(moveList, that.moveList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type, moveList);
    }
}
