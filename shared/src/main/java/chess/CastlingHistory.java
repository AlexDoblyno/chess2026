package chess;

public class CastlingHistory {
    boolean WHITEKingRookMoved;
    boolean WHITEQueenRookMoved;
    boolean BLACKKingRookMoved;
    boolean BLACKQueenRookMoved;
    boolean WHITEKingMoved;
    boolean BLACKKingMoved;

    CastlingHistory() {
        WHITEKingMoved = false;
        BLACKKingMoved = false;
        WHITEKingRookMoved = false;
        BLACKKingRookMoved = false;
        WHITEQueenRookMoved = false;
        BLACKQueenRookMoved = false;
    } //王车易位前，检测王与车有无移动，若是先前移动，无法王车易位

    public void resetHistory() {
        WHITEKingMoved = false;
        BLACKKingMoved = false;
        WHITEKingRookMoved = false;
        BLACKKingRookMoved = false;
        WHITEQueenRookMoved = false;
        BLACKQueenRookMoved = false;
    }
    public boolean isWHITEKingRookMoved() {
        return  WHITEKingRookMoved;
    }

    public void setWHITEKingRookMoved(boolean WHITEKingRookMoved) {
        this.WHITEKingRookMoved =  WHITEKingRookMoved;
    }

    public boolean isWHITEQueenRookMoved() {
        return WHITEQueenRookMoved;
    }

    public void setWHITEQueenRookMoved(boolean WHITEQueenRookMoved) {
        this.WHITEQueenRookMoved = WHITEQueenRookMoved;
    }

    public boolean isBLACKKingRookMoved() {
        return BLACKKingRookMoved;
    }

    public void setBLACKKingRookMoved(boolean BLACKKingRookMoved) {
        this.BLACKKingRookMoved = BLACKKingRookMoved;
    }

    public boolean isBLACKQueenRookMoved() {
        return BLACKQueenRookMoved;
    }

    public void setBLACKQueenRookMoved(boolean  BLACKQueenRookMoved) {
        this. BLACKQueenRookMoved =  BLACKQueenRookMoved;
    }

    public boolean isWHITEKingMoved() {
        return WHITEKingMoved;
    }

    public void setWHITEKingMoved(boolean WHITEKingMoved) {
        this.WHITEKingMoved = WHITEKingMoved;
    }

    public boolean isBLACKKingMoved () {
        return BLACKKingMoved;
    }

    public void setBLACKKingMoved (boolean BLACKKingMoved ) {
        this.BLACKKingMoved = BLACKKingMoved ;
    }
}