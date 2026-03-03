package client;

import chess.ChessGame.TeamColor;
import exception.ResponseException;
import models.AuthTokenData;
import models.GameData;
import models.UserData;
import org.junit.jupiter.api.*;
import server.Server;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;  // 修复为正确的类名 Server
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        try {
            server = new Server();  // 初始化 Server 实例
            var port = server.run(0);  // 启动服务器，并随机分配一个端口
            assertTrue(port > 0, "Server failed to start with a valid port.");
            System.out.println("Started test HTTP server on " + port);

            facade = new ServerFacade("http://localhost:" + port);
        } catch (Exception e) {
            fail("Failed to initialize the server: " + e.getMessage());
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            try {
                server.stop();  // 停止服务器
                System.out.println("Stopped HTTP server.");
            } catch (Exception e) {
                System.err.println("Failed to stop the server: " + e.getMessage());
            }
        }
    }

    @BeforeEach
    public void clearDB() throws ResponseException {
        facade.clearDatabase();
    }
// 测试注册用户功能，输入合法的用户信息，检查是否成功返回有效的授权数据。

    @Test
    public void testRegisterUserPositive() throws ResponseException {
        var userData = new UserData("player1", "password", "player1@email.com");
        AuthTokenData authData = facade.registerUser(userData);

        assertNotNull(authData);
        assertNotNull(authData.authToken());
        assertTrue(authData.authToken().length() > 10, "Auth token should be sufficiently long.");
    }
    // 测试注册用户功能，输入空的用户信息，检查是否抛出响应异常。

    @Test
    public void testRegisterUserNegative() {
        var userData = new UserData("", "", "");
        assertThrows(ResponseException.class, () -> facade.registerUser(userData),
                "Registering with empty credentials should throw an exception.");
    }
    // 测试登录功能，注册用户后尝试登录，检查是否成功返回有效的授权数据。

    @Test
    public void testLoginUserPositive() throws ResponseException {
        var userData = new UserData("player2", "password2", "player2@email.com");
        facade.registerUser(userData);
        var authData = facade.loginUser("player2", "password2");

        assertNotNull(authData);
        assertNotNull(authData.authToken());
        assertTrue(authData.authToken().length() > 10, "Auth token should be sufficiently long.");
    }
    // 测试登录功能，尝试使用不存在的用户或错误的密码登录，检查是否抛出响应异常。

    @Test
    public void testLoginUserNegative() {
        assertThrows(ResponseException.class, () -> facade.loginUser("nonexistentuser", "password"),
                "Logging in with invalid credentials should throw an exception.");
    }
    // 测试登出功能，使用有效的授权令牌登出，检查是否不会抛出异常。

    @Test
    public void testLogoutUserPositive() throws ResponseException {
        var userData = new UserData("player3", "password3", "player3@email.com");
        AuthTokenData authData = facade.registerUser(userData);
        assertDoesNotThrow(() -> facade.logoutUser(authData.authToken()));
    }
    // 测试登出功能，使用无效的授权令牌登出，检查是否抛出响应异常。

    @Test
    public void testLogoutUserNegative() {
        assertThrows(ResponseException.class, () -> facade.logoutUser("invalidAuthToken"),
                "Logging out with an invalid auth token should throw an exception.");
    }

    // 测试列出游戏功能，使用有效的授权令牌调用时，检查返回的游戏列表是否正确。
    @Test
    public void testListGamePositive() throws ResponseException {
        var userData = new UserData("player4", "password4", "player4@email.com");
        AuthTokenData authData = facade.registerUser(userData);
        Collection<GameData> games = assertDoesNotThrow(() -> facade.listGame(authData.authToken()));

        assertNotNull(games);
        assertTrue(games.isEmpty(), "Newly registered user should have no games listed.");
    }

    // 测试列出游戏功能，使用无效的授权令牌调用时，检查是否抛出响应异常。
    @Test
    public void testListGameNegative() {
        assertThrows(ResponseException.class, () -> facade.listGame("invalidAuthToken"),
                "Listing games with invalid auth token should throw an exception.");
    }

    // 测试创建游戏功能，使用有效的授权令牌创建游戏，检查是否成功返回游戏ID。
    @Test
    public void testCreateGamePositive() throws ResponseException {
        var userData = new UserData("player5", "password5", "player5@email.com");
        AuthTokenData authData = facade.registerUser(userData);
        var gameID = assertDoesNotThrow(() -> facade.createGame(authData.authToken(), "TestGame"));

        assertTrue(gameID > 0, "Game ID should be a positive integer.");
    }

    // 测试创建游戏功能，使用无效的授权令牌创建游戏，检查是否抛出响应异常。
    @Test
    public void testCreateGameNegative() {
        assertThrows(ResponseException.class, () -> facade.createGame("invalidAuthToken", "TestGame"),
                "Creating a game with invalid auth token should throw an exception.");
    }

    // 测试加入游戏功能，使用有效的授权令牌和游戏ID加入游戏，检查是否不会抛出异常。
    @Test
    public void testJoinGamePositive() throws ResponseException {
        var userData = new UserData("player6", "password6", "player6@email.com");
        AuthTokenData authData = facade.registerUser(userData);
        int gameID = facade.createGame(authData.authToken(), "GameToJoin");

        assertDoesNotThrow(() -> facade.joinGame(authData.authToken(), TeamColor.WHITE, gameID));
    }

    // 测试加入游戏功能，使用无效的授权令牌或无效的游戏ID加入游戏，检查是否抛出响应异常。
    @Test
    public void testJoinGameNegative() {
        assertThrows(ResponseException.class, () -> facade.joinGame("invalidAuthToken", TeamColor.WHITE, 12345),
                "Joining a game with invalid auth token should throw an exception.");
    }

    // 测试清空数据库功能，调用清理数据库的方法，检查是否不会抛出异常。
    @Test
    public void testClearDatabasePositive() {
        assertDoesNotThrow(() -> facade.clearDatabase(), "Clearing the database should not throw any exception.");
    }

    // 测试清空数据库功能，重复清理空的数据库，检查是否不会抛出异常。
    @Test
    public void testClearDatabaseNegative() {
        // Assuming the server handles clearing the database with invalid cases gracefully.
        assertDoesNotThrow(() -> facade.clearDatabase(),
                "Clearing the database multiple times should not throw an exception.");
    }
}