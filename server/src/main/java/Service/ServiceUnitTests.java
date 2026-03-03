package service;

import Models.AuthTokenData;
import Models.GameData;
import Models.UserData;
import chess.ChessGame;
import dataaccess.AuthDataAccess;
import dataaccess.GameDataAccess;
import dataaccess.UserDataAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.ServerException;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {
    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
        service.userDataAccess = new UserDataAccess();
        service.authDataAccess = new AuthDataAccess();
        service.gameDataAccess = new GameDataAccess();
    }

    // 1. 测试 register 方法
    @Test
    void testRegister_success() throws ServerException {
        UserData userData = new UserData("user1", "password1");
        AuthTokenData authToken = service.register(userData);

        assertNotNull(authToken);
        assertEquals("user1", authToken.username());
        assertNotNull(authToken.authToken());
    }

    @Test
    void testRegister_usernameTaken() {
        UserData userData = new UserData("user1", "password1");
        assertDoesNotThrow(() -> service.register(userData));

        ServerException exception = assertThrows(ServerException.class, () -> service.register(userData));
        assertEquals("already taken", exception.getMessage());
    }

    // 2. 测试 login 方法
    @Test
    void testLogin_success() throws ServerException {
        UserData user = new UserData("user1", "pass123");
        service.userDataAccess.addUserData(user);

        AuthTokenData authToken = service.login("user1", "pass123");

        assertNotNull(authToken);
        assertEquals("user1", authToken.username());
    }

    @Test
    void testLogin_invalidUsername() {
        ServerException exception = assertThrows(ServerException.class, () -> service.login("unknownUser", "password"));
        assertEquals("unauthorized", exception.getMessage());
    }

    @Test
    void testLogin_invalidPassword() {
        UserData user = new UserData("user1", "pass123");
        service.userDataAccess.addUserData(user);

        ServerException exception = assertThrows(ServerException.class, () -> service.login("user1", "wrongPassword"));
        assertEquals("unauthorized", exception.getMessage());
    }

    // 3. 测试 logOut 方法
    @Test
    void testLogOut_success() throws ServerException {
        AuthTokenData auth = new AuthTokenData("token123", "user1");
        service.authDataAccess.addAuthData(auth);

        assertDoesNotThrow(() -> service.logOut("token123"));
    }

    @Test
    void testLogOut_unauthorized() {
        ServerException exception = assertThrows(ServerException.class, () -> service.logOut("unknownToken"));
        assertEquals("unauthorized", exception.getMessage());
    }

    // 4. 测试 listGames 方法
    @Test
    void testListGames_success() throws ServerException {
        AuthTokenData auth = new AuthTokenData("token123", "user1");
        service.authDataAccess.addAuthData(auth);

        Collection<GameData> games = service.listGames("token123");
        assertNotNull(games);
        assertEquals(0, games.size());
    }

    @Test
    void testListGames_unauthorized() {
        ServerException exception = assertThrows(ServerException.class, () -> service.listGames("unknownToken"));
        assertEquals("unauthorized", exception.getMessage());
    }

    // 5. 测试 createGame 方法
    @Test
    void testCreateGame_success() throws ServerException {
        AuthTokenData auth = new AuthTokenData("token123", "user1");
        service.authDataAccess.addAuthData(auth);

        int gameID = service.createGame("token123", "newGame");
        assertNotNull(service.gameDataAccess.getGameByID(gameID));
    }

    @Test
    void testCreateGame_alreadyTaken() throws ServerException {
        AuthTokenData auth = new AuthTokenData("token123", "user1");
        service.authDataAccess.addAuthData(auth);

        service.createGame("token123", "newGame");
        ServerException exception = assertThrows(ServerException.class, () -> service.createGame("token123", "newGame"));
        assertEquals("already taken", exception.getMessage());
    }

    @Test
    void testCreateGame_unauthorized() {
        ServerException exception = assertThrows(ServerException.class, () -> service.createGame("invalidToken", "newGame"));
        assertEquals("unauthorized", exception.getMessage());
    }

    // 可按此结构继续添加其他 Service 方法的测试
}