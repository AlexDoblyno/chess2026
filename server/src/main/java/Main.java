import chess.*;
import dataaccess.DataAccessException;
import dataaccess.ServerException;
import server.Server;

public class Main {
    public static void main(String[] args) throws ServerException, DataAccessException {
        Server testServer = new Server();
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Server: " + piece);
        testServer.run(8080);
    }
}