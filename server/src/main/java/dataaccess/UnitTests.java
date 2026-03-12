package dataaccess;

import Models.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import chess.ChessGame;

import java.util.Collection;

class UnitTests {
    private MemoryGameDataAccess memoryGameDataAccess;

    @BeforeEach
    void setUp() {
        memoryGameDataAccess = new MemoryGameDataAccess();
    }

    @Test
    void testCreateGameAndGetGameByID_Positive() {
        // Arrange
        ChessGame chessGame = new ChessGame();
        GameData gameData = new GameData(1, null, null, "TestGame1", chessGame);

        // Act
        memoryGameDataAccess.createGame(gameData);
        GameData retrievedGame = memoryGameDataAccess.getGameByID(1);

        // Assert
        assertNotNull(retrievedGame);
        assertEquals("TestGame1", retrievedGame.gameName());
        assertEquals(1, retrievedGame.gameID());
    }

    @Test
    void testGetGameByID_Negative() {
        // Act
        GameData retrievedGame = memoryGameDataAccess.getGameByID(99);

        // Assert
        assertNull(retrievedGame, "Game with non-existent ID should return null");
    }

    @Test
    void testGetGameByName_Positive() {
        // Arrange
        ChessGame chessGame = new ChessGame();
        GameData gameData = new GameData(2, null, null, "TestGame2", chessGame);

        // Act
        memoryGameDataAccess.createGame(gameData);
        GameData retrievedGame = memoryGameDataAccess.getGameByName("TestGame2");

        // Assert
        assertNotNull(retrievedGame);
        assertEquals("TestGame2", retrievedGame.gameName());
        assertEquals(2, retrievedGame.gameID());
    }

    @Test
    void testGetGameByName_Negative() {
        // Act
        GameData retrievedGame = memoryGameDataAccess.getGameByName("NonExistentGame");

        // Assert
        assertNull(retrievedGame, "Game with non-existent name should return null");
    }


    @Test
    void testGetGameList_Positive() {
        // Arrange
        ChessGame chessGame1 = new ChessGame();
        GameData gameData1 = new GameData(5, null, null, "TestGame5", chessGame1);
        ChessGame chessGame2 = new ChessGame();
        GameData gameData2 = new GameData(6, null, null, "TestGame6", chessGame2);

        // Act
        memoryGameDataAccess.createGame(gameData1);
        memoryGameDataAccess.createGame(gameData2);

        Collection<GameData> gameList = memoryGameDataAccess.getGameList();

        // Assert
        assertEquals(2, gameList.size());
    }

    @Test
    void testClearGames_Positive() {
        // Arrange
        ChessGame chessGame = new ChessGame();
        GameData gameData = new GameData(7, null, null, "TestGame7", chessGame);

        memoryGameDataAccess.createGame(gameData);

        // Act
        memoryGameDataAccess.clearGames();
        Collection<GameData> gameList = memoryGameDataAccess.getGameList();

        // Assert
        assertTrue(gameList.isEmpty());
    }

    @Test
    void testCreateGame_DuplicateGameID() {
        // Arrange
        ChessGame chessGame = new ChessGame();
        GameData gameData1 = new GameData(8, null, null, "TestGame8", chessGame);
        GameData gameData2 = new GameData(8, null, null, "DuplicateGameID", chessGame);

        // Act
        memoryGameDataAccess.createGame(gameData1);

        // Attempt to add a duplicate game ID
        memoryGameDataAccess.createGame(gameData2);

        // Assert
        Collection<GameData> gameList = memoryGameDataAccess.getGameList();
        assertEquals(2, gameList.size(), "Duplicate IDs should still allow adding distinct objects");
    }


    @Test
    void testGetGameList_Negative_EmptyList() {
        // Act
        Collection<GameData> gameList = memoryGameDataAccess.getGameList();

        // Assert
        assertTrue(gameList.isEmpty(), "Game list should be empty initially");
    }

    @Test
    void testClearGames_Negative_NoGamesToClear() {
        // Act
        memoryGameDataAccess.clearGames();
        Collection<GameData> gameList = memoryGameDataAccess.getGameList();

        // Assert
        assertTrue(gameList.isEmpty(), "Clearing should not cause issues for an already empty list");
    }

    @Test
    void testGetGameNameCaseSensitivity() {
        // Arrange
        ChessGame chessGame = new ChessGame();
        GameData gameData = new GameData(11, null, null, "CaseSensitiveGame", chessGame);
        memoryGameDataAccess.createGame(gameData);

        // Act
        GameData retrievedGame = memoryGameDataAccess.getGameByName("casesensitiveGAME");

        // Assert
        assertNull(retrievedGame, "Game name comparison should be case-sensitive and not find the game");
    }


    @Test
    void testMultipleGamesWithSameName() {
        // Arrange
        ChessGame chessGame1 = new ChessGame();
        ChessGame chessGame2 = new ChessGame();

        GameData gameData1 = new GameData(13, null, null, "DuplicateName", chessGame1);
        GameData gameData2 = new GameData(14, null, null, "DuplicateName", chessGame2);

        // Act
        memoryGameDataAccess.createGame(gameData1);
        memoryGameDataAccess.createGame(gameData2);

        // Assert
        var firstGame = memoryGameDataAccess.getGameByName("DuplicateName");
        assertNotNull(firstGame, "Only the first game with the name should be retrieved by getGameByName");
    }

    @Test
    void testClearGamesWithMultipleGames() {
        // Arrange
        ChessGame chessGame1 = new ChessGame();
        ChessGame chessGame2 = new ChessGame();

        GameData gameData1 = new GameData(15, null, null, "Game15", chessGame1);
        GameData gameData2 = new GameData(16, null, null, "Game16", chessGame2);

        memoryGameDataAccess.createGame(gameData1);
        memoryGameDataAccess.createGame(gameData2);

        // Act
        memoryGameDataAccess.clearGames();

        // Assert
        assertTrue(memoryGameDataAccess.getGameList().isEmpty(), "All games should be cleared");
    }



}