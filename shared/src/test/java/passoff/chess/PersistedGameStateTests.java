package passoff.chess;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PersistedGameStateTests {

    private static final ChessMove WHITE_F3 = new ChessMove(new ChessPosition(2, 6), new ChessPosition(3, 6), null);
    private static final ChessMove BLACK_E5 = new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null);
    private static final ChessMove WHITE_G4 = new ChessMove(new ChessPosition(2, 7), new ChessPosition(4, 7), null);
    private static final ChessMove BLACK_QH4 = new ChessMove(new ChessPosition(8, 4), new ChessPosition(4, 8), null);

    @Test
    @DisplayName("Resigned State Survives Serialization")
    public void resignedStateSurvivesSerialization() {
        Gson gson = new Gson();
        ChessGame originalGame = new ChessGame();
        originalGame.markResigned();

        ChessGame restoredGame = gson.fromJson(gson.toJson(originalGame), ChessGame.class);

        Assertions.assertTrue(restoredGame.isResigned(), "resign state should survive Gson serialization");
        Assertions.assertTrue(restoredGame.isGameOver(), "restored resigned game should still be over");
        Assertions.assertThrows(InvalidMoveException.class,
                () -> restoredGame.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null)));
    }

    @Test
    @DisplayName("Checkmate State Survives Serialization")
    public void checkmateStateSurvivesSerialization() throws InvalidMoveException {
        Gson gson = new Gson();
        ChessGame originalGame = new ChessGame();

        originalGame.makeMove(WHITE_F3);
        originalGame.makeMove(BLACK_E5);
        originalGame.makeMove(WHITE_G4);
        originalGame.makeMove(BLACK_QH4);
        originalGame.refreshGameStatus();

        ChessGame restoredGame = gson.fromJson(gson.toJson(originalGame), ChessGame.class);

        Assertions.assertTrue(restoredGame.isGameOver(), "checkmated game should stay over after Gson serialization");
        Assertions.assertEquals(ChessGame.GameStatus.CHECKMATE, restoredGame.getGameStatus(),
                "serialized game should keep the checkmate status");
        Assertions.assertThrows(InvalidMoveException.class,
                () -> restoredGame.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null)));
    }
}