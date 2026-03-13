package dataaccess;

import models.AuthTokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthDAOTests {
    private MemoryAuthDataAccess authDataAccess;

    @BeforeEach
    void setUp() {
        authDataAccess = new MemoryAuthDataAccess();
    }

    // ==========================================
    // 1. addAuthData Tests
    // ==========================================
    @Test
    void testAddAuthData_Positive() {
        AuthTokenData authData = new AuthTokenData("token123", "User1");
        authDataAccess.addAuthData(authData);

        AuthTokenData retrieved = authDataAccess.getAuthData("token123");
        assertNotNull(retrieved, "Should be able to retrieve the added auth data");
        assertEquals("User1", retrieved.username());
    }

    @Test
    void testAddAuthData_Negative() {
        // 根据你 MemoryAuthDataAccess 的逻辑，传入 null 会抛出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            authDataAccess.addAuthData(null);
        }, "Adding null auth data should throw NullPointerException");
    }

    // ==========================================
    // 2. getAuthData Tests
    // ==========================================
    @Test
    void testGetAuthData_Positive() {
        AuthTokenData authData = new AuthTokenData("token456", "User2");
        authDataAccess.addAuthData(authData);

        AuthTokenData retrieved = authDataAccess.getAuthData("token456");
        assertNotNull(retrieved);
        assertEquals("token456", retrieved.authToken());
    }

    @Test
    void testGetAuthData_Negative() {
        // 查找不存在的 Token 应该返回 null
        AuthTokenData retrieved = authDataAccess.getAuthData("NonExistentToken");
        assertNull(retrieved, "Retrieving a non-existent token should return null");
    }

    // ==========================================
    // 3. removeAuthData Tests
    // ==========================================
    @Test
    void testRemoveAuthData_Positive() {
        AuthTokenData authData = new AuthTokenData("token789", "User3");
        authDataAccess.addAuthData(authData);

        // 确认已添加
        assertNotNull(authDataAccess.getAuthData("token789"));

        // 执行删除
        authDataAccess.removeAuthData(authData);

        // 确认已删除
        assertNull(authDataAccess.getAuthData("token789"), "Auth data should be removed");
    }

    @Test
    void testRemoveAuthData_Negative() {
        AuthTokenData authData = new AuthTokenData("token111", "User4");
        authDataAccess.addAuthData(authData);

        // 尝试删除一个数据库里根本没有的 token
        AuthTokenData fakeData = new AuthTokenData("fakeToken", "FakeUser");
        authDataAccess.removeAuthData(fakeData);

        // 负面测试断言：尝试删除不存在的元素不应该影响现有的数据，size 仍为 1
        assertEquals(1, authDataAccess.authTokenDatabase.size(), "Removing non-existent data should not affect the database");
    }

    // ==========================================
    // 4. clearAuthTokens Test (Clear 只需要 Positive)
    // ==========================================
    @Test
    void testClearAuthTokens_Positive() {
        authDataAccess.addAuthData(new AuthTokenData("tokenA", "UserA"));
        authDataAccess.addAuthData(new AuthTokenData("tokenB", "UserB"));

        authDataAccess.clearAuthTokens();

        assertTrue(authDataAccess.authTokenDatabase.isEmpty(), "Database should be empty after clear");
    }
}