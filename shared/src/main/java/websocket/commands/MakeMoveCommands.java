package websocket.commands;

import chess.ChessBoard;
import chess.ChessMove;

public class MakeMoveCommands extends UserGameCommand {
    private final ChessMove move;

    public MakeMoveCommands(CommandType commandType, String authToken, Integer gameID, ChessMove move) {
        super(commandType, authToken, gameID);
        this.move = move;
    }

    public ChessMove getMove() {
        return move;
    }
}
