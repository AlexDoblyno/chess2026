package dataaccess;

import Models.AuthTokenData;

public interface AuthDataAccess {
    /**
     * AuthData methods
     */
    void addAuthData(AuthTokenData authData) throws ServerException, server.ServerException;

    void removeAuthData(AuthTokenData authData) throws ServerException, server.ServerException;

    AuthTokenData getAuthData(String authData) throws ServerException, server.ServerException;

    /**
     * Mass deletion methods
     */
    void clearAuthTokens() throws ServerException;
}