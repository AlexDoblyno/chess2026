package dataaccess;

import models.AuthTokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class UnitTest {

    private MemoryAuthDataAccess authDataAccess;

    @BeforeEach
    public void setUp() {
        authDataAccess = new MemoryAuthDataAccess();
    }

    @Test
    public void testAddAuthDataPositive() {
        AuthTokenData authData = new AuthTokenData("token1", "user1");
        authDataAccess.addAuthData(authData);

        assertNotNull(authDataAccess.getAuthData("token1"), "AuthToken should be added successfully.");
        assertEquals("user1", authDataAccess.getAuthData("token1").username(), "Stored username should match.");
    }

    @Test
    public void testAddAuthDataNegative() {
        AuthTokenData authData = new AuthTokenData("token1", "user1");
        authDataAccess.addAuthData(authData);
        authDataAccess.addAuthData(authData);

        assertNotNull(authDataAccess.getAuthData("token1"), "AuthToken should be added successfully.");
        assertEquals("user1", authDataAccess.getAuthData("token1").username(), "Stored username should match.");
    }

    @Test
    public void testRemoveAuthDataPositive() {
        AuthTokenData authData = new AuthTokenData("token1", "user1");
        authDataAccess.addAuthData(authData);
        authDataAccess.removeAuthData(authData);

        assertNull(authDataAccess.getAuthData("token1"), "AuthToken should be removed successfully.");
    }

    @Test
    public void testRemoveAuthDataNegative() {
        AuthTokenData authData = new AuthTokenData("token1", "user1");

        authDataAccess.removeAuthData(authData); // Remove non-existent token

        assertNull(authDataAccess.getAuthData("token1"), "Removing non-existent AuthToken should have no effect.");
    }

    @Test
    public void testGetAuthDataPositive() {
        AuthTokenData authData = new AuthTokenData("token1", "user1");
        authDataAccess.addAuthData(authData);

        AuthTokenData retrievedData = authDataAccess.getAuthData("token1");
        assertNotNull(retrievedData, "AuthToken should be retrieved successfully.");
        assertEquals(authData, retrievedData, "Retrieved AuthToken should match the added AuthToken.");
    }

    @Test
    public void testGetAuthDataNegative() {
        assertNull(authDataAccess.getAuthData("nonExistentToken"), "Should return null for non-existent token.");
    }

    @Test
    public void testClearAuthTokens() {
        authDataAccess.addAuthData(new AuthTokenData("token1", "user1"));
        authDataAccess.addAuthData(new AuthTokenData("token2", "user2"));

        authDataAccess.clearAuthTokens();

        assertTrue(authDataAccess.authTokenDatabase.isEmpty(), "AuthToken database should be empty after clearing.");
    }

    @Test
    public void testAddNullAuthDataNegative() {
        assertThrows(NullPointerException.class, () ->
                authDataAccess.addAuthData(null), "Adding null AuthTokenData should throw NullPointerException.");
    }

    @Test
    public void testClearAndAccessAfterClear() {
        AuthTokenData authData = new AuthTokenData("token1", "user1");
        authDataAccess.addAuthData(authData);

        authDataAccess.clearAuthTokens();

        assertNull(authDataAccess.getAuthData("token1"), "AuthToken should not exist after clearing.");
        assertDoesNotThrow(() -> authDataAccess.removeAuthData(authData), "Should not throw when removing from an empty database.");
    }
}
