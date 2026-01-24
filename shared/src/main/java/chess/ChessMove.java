package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {

    private ChessPosition StartPosition;
    private ChessPosition EndPosition;
    private ChessPiece.PieceType promotionPiece;

    public ChessMove(ChessPosition StartPosition, ChessPosition EndPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.StartPosition = StartPosition;
        this.EndPosition = EndPosition;
        this. promotionPiece = promotionPiece;
    }

    /** New constructor with null promotionpiece object
     *
     * @param StartPosition what it looks like
     * @param EndPosition what it sounds like
     */
    public ChessMove(ChessPosition StartPosition, ChessPosition EndPosition) {
        this.StartPosition = StartPosition;
        this.EndPosition = EndPosition;
        this. promotionPiece = null;
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return StartPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return EndPosition;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChessMove chessMove)) {
            return false;
        }
        return Objects.equals(StartPosition, chessMove.StartPosition) && Objects.equals(EndPosition, chessMove.EndPosition) && promotionPiece == chessMove.promotionPiece;
    }

    @Override
    public int hashCode() {
        return Objects.hash(StartPosition, EndPosition, promotionPiece);
    }
}