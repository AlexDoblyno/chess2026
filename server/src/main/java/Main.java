import chess.*;
import server.Server;

public class Main {
    public static void main(String[] args) {
        Server testServer = new Server();
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Server: " + piece);
        testServer.run(8080);
    }
}