import chess.*;
import client.ChessClient;
import ui.BaseUI;
import ui.PreloginUI;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece + "\nPlease register or log in! Type 'help' for a list of commands.");

        String serverUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        ChessClient client = new ChessClient(serverUrl);
        BaseUI currentUI = new PreloginUI(client);

        while (currentUI != null)
            currentUI = currentUI.run();
    }
}