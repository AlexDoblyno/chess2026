package service;

import dataaccess.DataAccessException;
import models.AuthTokenData;
import models.GameData;
import models.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.ServerException;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ServiceUnitTests {

    private Service service;

    @BeforeEach
    void setUp() throws dataaccess.ServerException, DataAccessException {
        service = new Service();
    }

    // **Test cases for register()**
    @Test
    void registerPositiveTest() throws ServerException {
        // 正向测试：注册一个新用户
        UserData user = new UserData("validUser", "password123", "sliu61@byu.edu");
        AuthTokenData authToken = service.register(user);
        assertNotNull(authToken);
        assertEquals("validUser", authToken.username());
    }

    @Test
    void registerNegativeTest() {
        // 负向测试：重复注册相同用户名
        UserData user = new UserData("duplicateUser", "password123", "sliu61@byu.edu");
        assertDoesNotThrow(() -> service.register(user));

        ServerException exception = assertThrows(ServerException.class, () -> service.register(user));
        assertEquals(403, exception.getStatusCode());
        assertEquals("already taken", exception.getMessage());
    }

    // **Test cases for login()**
    @Test
    void loginPositiveTest() throws ServerException {
        // 正向测试：登录已注册用户
        UserData user = new UserData("validLogin", "password123", "sliu61@byu.edu");
        service.register(user);

        AuthTokenData authToken = service.login("validLogin", "password123");
        assertNotNull(authToken);
        assertEquals("validLogin", authToken.username());
    }

    @Test
    void loginNegativeTest() throws ServerException {
        // 负向测试：登录时使用错误的密码
        UserData user = new UserData("loginUser", "password123", "sliu61@byu.edu");
        service.register(user); // 确定用户注册成功

        // 测试使用错误的密码登录，断定会抛出异常
        ServerException exception = assertThrows(ServerException.class, () -> service.login("loginUser", "wrongPassword"));
        assertEquals(401, exception.getStatusCode()); // 验证状态码是否符合预期
    }

    // **Test cases for logOut()**
    @Test
    void logoutPositiveTest() throws ServerException {
        // 正向测试：成功注销登录
        UserData user = new UserData("logOutUser", "password123", "sliu61@byu.edu");
        AuthTokenData authToken = service.register(user);

        assertDoesNotThrow(() -> service.logOut(authToken.authToken()));
    }

    @Test
    void logoutNegativeTest() {
        // 负向测试：注销无效 token
        ServerException exception = assertThrows(ServerException.class, () -> service.logOut("invalidToken"));
        assertEquals(401, exception.getStatusCode());
    }

    // **Test cases for listGames()**
    @Test
    void listGamesPositiveTest() throws ServerException {
        // 正向测试：列出已创建的游戏
        UserData user = new UserData("listGamesUser", "password123", "sliu61@byu.edu");
        AuthTokenData authToken = service.register(user);

        Collection<GameData> games = service.listGames(authToken.authToken());
        assertNotNull(games);
        assertEquals(0, games.size()); // 初始状态应该没有任何游戏
    }

    @Test
    void listGamesNegativeTest() {
        // 负向测试：列出游戏时使用无效 token
        ServerException exception = assertThrows(ServerException.class, () -> service.listGames("invalidToken"));
        assertEquals(401, exception.getStatusCode());
    }

    // **Test cases for createGame()**
    @Test
    void createGamePositiveTest() throws ServerException {
        // 正向测试：成功创建一个新游戏
        UserData user = new UserData("createGameUser", "password123", "sliu61@byu.edu");
        AuthTokenData authToken = service.register(user);

        int gameID = service.createGame(authToken.authToken(), "TestGame");
        assertTrue(gameID > 0);
    }

    @Test
    void createGameNegativeTest() throws ServerException {
        // 注册用户并获取有效的 AuthToken
        UserData user = new UserData("negativeCreateGameUser", "password123", "sliu61@byu.edu");
        AuthTokenData authToken = service.register(user); // 假设 register 已正确抛出异常或成功返回

        // 测试 createGame 传入 null 游戏名称的情况
        ServerException exception = assertThrows(ServerException.class, () ->
                service.createGame(authToken.authToken(), null)
        );

        // 验证异常状态码和消息内容
        assertEquals(400, exception.getStatusCode());
        assertEquals("bad request", exception.getMessage());
    }
    // **Test cases for clearApp()**
    @Test
    void clearAppPositiveTest() throws ServerException {
        // 正向测试：清空数据库
        UserData user = new UserData("clearAppUser", "password123", "sliu61@byu.edu");
        AuthTokenData authToken = service.register(user);

        service.createGame(authToken.authToken(), "GameToClear");
        service.clearApp();

        // 调用清空后 listGames 应该报错，数据已被清空
        ServerException exception = assertThrows(ServerException.class, () -> service.listGames(authToken.authToken()));
        assertEquals(401, exception.getStatusCode());
    }

    @Test
    void clearAppNegativeTest() throws ServerException {
        // 负向测试：清空已清空的数据库
        service.clearApp(); // 确保数据库是空的
        assertDoesNotThrow(() -> service.clearApp()); // 再次调用也应该不抛出异常
    }
}
//......,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
