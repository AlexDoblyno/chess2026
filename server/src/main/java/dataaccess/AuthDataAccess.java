package dataaccess;

import models.AuthTokenData;

public interface AuthDataAccess {


     // AuthData Persistence Operations

    void addAuthData(AuthTokenData authData) throws ServerException, server.ServerException;
    void clearAuthTokens() throws ServerException;
    void removeAuthData(AuthTokenData authData) throws ServerException, server.ServerException;

    AuthTokenData getAuthData(String authData) throws ServerException, server.ServerException;

    // ----------------------------------------------------



}