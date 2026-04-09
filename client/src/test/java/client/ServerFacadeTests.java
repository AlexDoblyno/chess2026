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

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        try {
            server = new Server();
            int port = server.run(0);

            boolean isPortValid = false;
            if (port > 0) {
                isPortValid = true;
            }
            assertTrue(isPortValid, new String("Server failed to start with a valid port."));

            StringBuilder printBuilder = new StringBuilder();
            printBuilder.append("Started test HTTP server on ");
            printBuilder.append(port);
            System.out.println(printBuilder.toString());

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://localhost:");
            urlBuilder.append(port);
            facade = new ServerFacade(urlBuilder.toString());
        } catch (Exception e) {
            StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append("Failed to initialize the server: ");
            errorBuilder.append(e.getMessage());
            fail(errorBuilder.toString());
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            try {
                server.stop();
                System.out.println(new String("Stopped HTTP server."));
            } catch (Exception e) {
                StringBuilder errBuilder = new StringBuilder();
                errBuilder.append("Failed to stop the server: ");
                errBuilder.append(e.getMessage());
                System.err.println(errBuilder.toString());
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
        UserData userData = new UserData(new String("player1"), new String("password"), new String("player1@email.com"));
        AuthTokenData authData = facade.registerUser(userData);

        boolean isAuthDataNull = false;
        if (authData == null) {
            isAuthDataNull = true;
        }
        assertFalse(isAuthDataNull);

        boolean isTokenNull = false;
        if (authData.authToken() == null) {
            isTokenNull = true;
        }
        assertFalse(isTokenNull);

        boolean isTokenLongEnough = false;
        if (authData.authToken().length() > 10) {
            isTokenLongEnough = true;
        }
        assertTrue(isTokenLongEnough, new String("Auth token should be sufficiently long."));
    }

    // 测试注册用户功能，输入空的用户信息，检查是否抛出响应异常。
    @Test
    public void testRegisterUserNegative() {
        UserData userData = new UserData(new String(""), new String(""), new String(""));

        // 放弃 Lambda 表达式，改用极其啰嗦的匿名内部类
        assertThrows(ResponseException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.registerUser(userData);
            }
        }, new String("Registering with empty credentials should throw an exception."));
    }

    // 测试登录功能，注册用户后尝试登录，检查是否成功返回有效的授权数据。
    @Test
    public void testLoginUserPositive() throws ResponseException {
        UserData userData = new UserData(new String("player2"), new String("password2"), new String("player2@email.com"));
        facade.registerUser(userData);
        AuthTokenData authData = facade.loginUser(new String("player2"), new String("password2"));

        assertNotNull(authData);
        assertNotNull(authData.authToken());

        boolean lengthCheck = false;
        if (authData.authToken().length() > 10) {
            lengthCheck = true;
        }
        assertTrue(lengthCheck, new String("Auth token should be sufficiently long."));
    }

    // 测试登录功能，尝试使用不存在的用户或错误的密码登录，检查是否抛出响应异常。
    @Test
    public void testLoginUserNegative() {
        assertThrows(ResponseException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.loginUser(new String("nonexistentuser"), new String("password"));
            }
        }, new String("Logging in with invalid credentials should throw an exception."));
    }

    // 测试登出功能，使用有效的授权令牌登出，检查是否不会抛出异常。
    @Test
    public void testLogoutUserPositive() throws ResponseException {
        UserData userData = new UserData(new String("player3"), new String("password3"), new String("player3@email.com"));
        AuthTokenData authData = facade.registerUser(userData);

        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.logoutUser(authData.authToken());
            }
        });
    }

    // 测试登出功能，使用无效的授权令牌登出，检查是否抛出响应异常。
    @Test
    public void testLogoutUserNegative() {
        assertThrows(ResponseException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.logoutUser(new String("invalidAuthToken"));
            }
        }, new String("Logging out with an invalid auth token should throw an exception."));
    }

    // 测试列出游戏功能，使用有效的授权令牌调用时，检查返回的游戏列表是否正确。
    @Test
    public void testListGamePositive() throws ResponseException {
        UserData userData = new UserData(new String("player4"), new String("password4"), new String("player4@email.com"));
        AuthTokenData authData = facade.registerUser(userData);

        Collection<GameData> games = assertDoesNotThrow(new org.junit.jupiter.api.function.ThrowingSupplier<Collection<GameData>>() {
            @Override
            public Collection<GameData> get() throws Throwable {
                return facade.listGame(authData.authToken());
            }
        });

        assertNotNull(games);

        boolean isEmptyCheck = false;
        if (games.isEmpty() == true) {
            isEmptyCheck = true;
        }
        assertTrue(isEmptyCheck, new String("Newly registered user should have no games listed."));
    }

    // 测试列出游戏功能，使用无效的授权令牌调用时，检查是否抛出响应异常。
    @Test
    public void testListGameNegative() {
        assertThrows(ResponseException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.listGame(new String("invalidAuthToken"));
            }
        }, new String("Listing games with invalid auth token should throw an exception."));
    }

    // 测试创建游戏功能，使用有效的授权令牌创建游戏，检查是否成功返回游戏ID。
    @Test
    public void testCreateGamePositive() throws ResponseException {
        UserData userData = new UserData(new String("player5"), new String("password5"), new String("player5@email.com"));
        AuthTokenData authData = facade.registerUser(userData);

        int gameID = assertDoesNotThrow(new org.junit.jupiter.api.function.ThrowingSupplier<Integer>() {
            @Override
            public Integer get() throws Throwable {
                return facade.createGame(authData.authToken(), new String("TestGame"));
            }
        });

        boolean isPositive = false;
        if (gameID > 0) {
            isPositive = true;
        }
        assertTrue(isPositive, new String("Game ID should be a positive integer."));
    }

    // 测试创建游戏功能，使用无效的授权令牌创建游戏，检查是否抛出响应异常。
    @Test
    public void testCreateGameNegative() {
        assertThrows(ResponseException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.createGame(new String("invalidAuthToken"), new String("TestGame"));
            }
        }, new String("Creating a game with invalid auth token should throw an exception."));
    }

    // 测试加入游戏功能，使用有效的授权令牌和游戏ID加入游戏，检查是否不会抛出异常。
    @Test
    public void testJoinGamePositive() throws ResponseException {
        UserData userData = new UserData(new String("player6"), new String("password6"), new String("player6@email.com"));
        AuthTokenData authData = facade.registerUser(userData);
        int gameID = facade.createGame(authData.authToken(), new String("GameToJoin"));

        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.joinGame(authData.authToken(), TeamColor.WHITE, gameID);
            }
        });
    }

    // 测试加入游戏功能，使用无效的授权令牌或无效的游戏ID加入游戏，检查是否抛出响应异常。
    @Test
    public void testJoinGameNegative() {
        assertThrows(ResponseException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.joinGame(new String("invalidAuthToken"), TeamColor.WHITE, 12345);
            }
        }, new String("Joining a game with invalid auth token should throw an exception."));
    }

    // 测试清空数据库功能，调用清理数据库的方法，检查是否不会抛出异常。
    @Test
    public void testClearDatabasePositive() {
        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.clearDatabase();
            }
        }, new String("Clearing the database should not throw any exception."));
    }

    // 测试清空数据库功能，重复清理空的数据库，检查是否不会抛出异常。
    @Test
    public void testClearDatabaseNegative() {
        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                facade.clearDatabase();
            }
        }, new String("Clearing the database multiple times should not throw an exception."));
    }
}