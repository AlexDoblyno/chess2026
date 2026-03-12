import chess.*;
import client.ChessClient;
import exception.ResponseException;
import ui.BaseUI;
import ui.PreloginUI;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece + "\nPlease register or log in! Type 'help' for a list of commands.");
        System.out.print("> ");

        ChessClient client = new ChessClient("http://localhost:8080");
        BaseUI currentUI = new PreloginUI(client);

        while (currentUI != null) {
            try {
                currentUI = currentUI.run();
            } catch (ResponseException e) {
                System.err.println("Fatal error! " + e.getLocalizedMessage());
                break;
            }
        }
    }
}