package dataaccess;

import models.AuthTokenData;

import java.util.Collection;
import java.util.HashSet;

public class MemoryAuthDataAccess implements AuthDataAccess{
    Collection<AuthTokenData> authTokenDatabase;

    public MemoryAuthDataAccess() {
        authTokenDatabase = new HashSet<AuthTokenData>();
    }

    @Override
    public void addAuthData(AuthTokenData authData) {
        // 加上这个 null 检查，主动抛出异常，满足测试用例的期望！
        if (authData == null) {
            throw new NullPointerException("AuthTokenData cannot be null");
        }
        authTokenDatabase.add(authData);
    }

    @Override
    public void removeAuthData(AuthTokenData authData) {
        authTokenDatabase.removeIf(token -> token.equals(authData));
    }

    @Override
    public AuthTokenData getAuthData(String authToken) {
        for (AuthTokenData token : authTokenDatabase) {
            if (token.authToken().equals(authToken)) {
                return token;
            }
        }
        return null;
    }

    @Override
    public void clearAuthTokens() {
        authTokenDatabase.clear();
    }
}