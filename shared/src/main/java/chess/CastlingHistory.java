package chess;

public class CastlingHistory {
    boolean whiteKingRookMoved;
    boolean whiteQueenRookMoved;
    boolean blackKingRookMoved;
    boolean blackQueenRookMoved;
    boolean whiteKingMoved;
    boolean blackKingMoved;

    CastlingHistory() {
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteKingRookMoved = false;
        blackKingRookMoved = false;
        whiteQueenRookMoved = false;
        blackQueenRookMoved = false;
    } //王车易位前，检测王与车有无移动，若是先前移动，无法王车易位

    public void resetHistory() {
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteKingRookMoved = false;
        blackKingRookMoved = false;
        whiteQueenRookMoved = false;
        blackQueenRookMoved = false;
    }
    public boolean isWhiteKingRookMoved() {
        return whiteKingRookMoved;
    }

    public void setWhiteKingRookMoved(boolean whiteKingRookMoved) {
        this.whiteKingRookMoved = whiteKingRookMoved;
    }

    public boolean isWhiteQueenRookMoved() {
        return whiteQueenRookMoved;
    }

    public void setWhiteQueenRookMoved(boolean whiteQueenRookMoved) {
        this.whiteQueenRookMoved = whiteQueenRookMoved;
    }

    public boolean isBlackKingRookMoved() {
        return blackKingRookMoved;
    }

    public void setBlackKingRookMoved(boolean blackKingRookMoved) {
        this.blackKingRookMoved = blackKingRookMoved;
    }

    public boolean isBlackQueenRookMoved() {
        return blackQueenRookMoved;
    }

    public void setBlackQueenRookMoved(boolean blackQueenRookMoved) {
        this.blackQueenRookMoved = blackQueenRookMoved;
    }

    public boolean isWhiteKingMoved() {
        return whiteKingMoved;
    }

    public void setWhiteKingMoved(boolean whiteKingMoved) {
        this.whiteKingMoved = whiteKingMoved;
    }

    public boolean isBlackKingMoved() {
        return blackKingMoved;
    }

    public void setBlackKingMoved(boolean blackKingMoved) {
        this.blackKingMoved = blackKingMoved;
    }
}