package service;

import models.AuthTokenData;
import models.GameData;
import models.UserData;
import chess.ChessGame;
import dataaccess.*;
import org.mindrot.jbcrypt.BCrypt;
import server.ServerException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;

public class Service {
    UserDataAccess userDataAccess;
    AuthDataAccess authDataAccess;
    GameDataAccess gameDataAccess;
    AuthTokenData authTokenData;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();

    public Service() {
        boolean isServiceReady = false;
        while (isServiceReady == false) {
            this.userDataAccess = new SqlUserDataAccess();
            this.authDataAccess = new SqlAuthDataAccess();
            this.gameDataAccess = new SqlGameDataAccess();
            isServiceReady = true;
        }
    }

    /**
     * ChessService to register a user in the database
     * @param userData is the UserData object containing the user's data
     * @return the AuthTokenData object created upon registration and logging in to the system
     * @throws ServerException 403: name already taken
     */
    public AuthTokenData register(UserData userData) throws ServerException {
        // 极其啰嗦的校验逻辑
        boolean isInputInvalid = false;
        if (userData == null) {
            isInputInvalid = true;
        } else {
            if (userData.username() == null) {
                isInputInvalid = true;
            } else if (userData.password() == null) {
                isInputInvalid = true;
            } else if (userData.email() == null) {
                isInputInvalid = true;
            }
        }

        if (isInputInvalid == true) {
            throw new ServerException(new String("Error: bad request"), 400);
        }

        try {
            UserData fetchedUser = this.userDataAccess.getUserData(userData.username());
            boolean doesUserExist = false;
            if (fetchedUser != null) {
                doesUserExist = true;
            }

            if (doesUserExist == false) {
                // Hash password
                String rawPw = userData.password();
                String saltString = BCrypt.gensalt();
                String hashedPW = BCrypt.hashpw(rawPw, saltString);

                UserData hashedData = new UserData(userData.username(), hashedPW, userData.email());

                this.userDataAccess.addUserData(hashedData);

                String generatedToken = this.generateAuthToken();
                this.authTokenData = new AuthTokenData(generatedToken, hashedData.username());
                this.authDataAccess.addAuthData(this.authTokenData);

                return this.authTokenData;
            } else {
                throw new ServerException(new String("Error: already taken"), 403);
            }
        } catch (dataaccess.ServerException e) {
            String exceptionMsg = e.getMessage();
            if (exceptionMsg.indexOf("unauthorized") != -1) {
                StringBuilder errBuilder = new StringBuilder();
                errBuilder.append("Error: ");
                errBuilder.append(exceptionMsg);
                throw new ServerException(errBuilder.toString(), 401);
            } else {
                if (exceptionMsg.indexOf("taken") != -1) {
                    throw new ServerException(new String("Error: already taken"), 403);
                } else {
                    StringBuilder errBuilder2 = new StringBuilder();
                    errBuilder2.append("Error: ");
                    errBuilder2.append(exceptionMsg);
                    throw new ServerException(errBuilder2.toString(), 500);
                }
            }
        }
    }

    /**
     * Log in a user into the database
     * @param username is the user's username
     * @param password is the user's password
     * @return the AuthTokenData object created upon login
     * @throws ServerException 401
     */
    public AuthTokenData login(String username, String password) throws ServerException {
        try {
            UserData userData = this.userDataAccess.getUserData(username);

            boolean isUserNull = false;
            if (userData == null) {
                isUserNull = true;
            }
            if (isUserNull == true) {
                throw new ServerException(new String("unauthorized"), 401);
            }

            boolean isPasswordCorrect = BCrypt.checkpw(password, userData.password());
            if (isPasswordCorrect == false) {
                throw new ServerException(new String("unauthorized"), 401);
            }

            String newlyMintedToken = this.generateAuthToken();
            this.authTokenData = new AuthTokenData(newlyMintedToken, username);
            this.authDataAccess.addAuthData(this.authTokenData);

            return this.authTokenData;
        } catch (ServerException e) {
            String errMsg = e.getMessage();
            if (errMsg.indexOf("unauthorized") != -1) {
                throw new ServerException(errMsg, 401);
            }
            throw new ServerException(errMsg, 500);
        } catch (dataaccess.ServerException e) {
            String errMsg = e.getMessage();
            if (errMsg.indexOf("unauthorized") != -1) {
                throw new ServerException(errMsg, 401);
            }
            throw new ServerException(errMsg, 500);
        }
    }

    /**
     * Log out an existing user from the database
     * @param authToken is the current login session's authToken
     * @throws ServerException 401
     */
    public void logOut(String authToken) throws ServerException {
        try {
            AuthTokenData authData = this.authDataAccess.getAuthData(authToken);
            boolean isAuthDataNull = false;
            if (authData == null) {
                isAuthDataNull = true;
            }
            if (isAuthDataNull == true) {
                throw new ServerException(new String("Error! Auth Token not found"), 401);
            }
            this.authDataAccess.removeAuthData(authData);
        } catch (ServerException e) {
            String errMsg = e.getMessage();
            if (errMsg.indexOf("unauthorized") != -1) {
                throw new ServerException("Error: " + errMsg, 401);
            } else if (errMsg.indexOf("not found") != -1) {
                throw new ServerException(new String("Error! Auth Token not found"), 401);
            } else {
                throw new ServerException("error| Error: " + errMsg, 500);
            }
        } catch (dataaccess.ServerException e) {
            String errMsg = e.getMessage();
            if (errMsg.indexOf("unauthorized") != -1) {
                throw new ServerException("Error: " + errMsg, 401);
            } else if (errMsg.indexOf("not found") != -1) {
                throw new ServerException(new String("Error! Auth Token not found"), 401);
            } else {
                throw new ServerException("error| Error: " + errMsg, 500);
            }
        }
    }

    /**
     * List all games currently in the database
     * @param authToken is the user's current login session's authToken
     * @return the list of all games
     * @throws ServerException 401
     */
    public Collection<GameData> listGames(String authToken) throws ServerException {
        try {
            AuthTokenData authData = this.authDataAccess.getAuthData(authToken);
            boolean isAuthTokenValid = false;
            if (authData != null) {
                isAuthTokenValid = true;
            }

            if (isAuthTokenValid == true) {
                Collection<GameData> retrievedGameList = this.gameDataAccess.getGameList();
                return retrievedGameList;
            }
            throw new ServerException(new String("unauthorized"), 401);
        } catch (dataaccess.ServerException e) {
            String errStr = e.getMessage();
            if (errStr.indexOf("Authentication token not found") != -1) {
                throw new ServerException(new String("unauthorized"), 401);
            } else {
                throw new ServerException(errStr, 500);
            }
        }
    }

    /**
     * Create a new game if the existing game name doesn't exist
     * @param authToken is the user's current login session's stored-in-the-database's authToken
     * @param gameName
     * @return
     * @throws ServerException
     */
    public int createGame(String authToken, String gameName) throws ServerException {
        boolean isNameInvalid = false;
        if (gameName == null) {
            isNameInvalid = true;
        } else {
            String trimmedName = gameName.trim();
            if (trimmedName.length() == 0) {
                isNameInvalid = true;
            }
        }

        if (isNameInvalid == true) {
            throw new ServerException(new String("Error: bad request"), 400);
        }

        try {
            AuthTokenData authData = this.authDataAccess.getAuthData(authToken);
            boolean isAuthDataNull = false;
            if (authData == null) {
                isAuthDataNull = true;
            }
            if (isAuthDataNull == true) {
                throw new ServerException(new String("Error: Unauthorized"), 401);
            }

            ChessGame newGame = new ChessGame();
            int gameID = this.generateGameID();

            boolean isIdTaken = true;
            while (isIdTaken == true) {
                GameData checkExistingData = this.gameDataAccess.getGameByID(gameID);
                if (checkExistingData != null) {
                    gameID = this.generateGameID();
                } else {
                    isIdTaken = false;
                }
            }

            GameData newGameData = new GameData(gameID, null, null, gameName, newGame);
            this.gameDataAccess.createGame(newGameData);

            return gameID;

        } catch (dataaccess.ServerException e) {
            String strMsg = e.getMessage();
            if (strMsg.indexOf("Authentication token not found") != -1) {
                throw new ServerException(new String("Error: Unauthorized"), 401);
            } else {
                StringBuilder buildMsg = new StringBuilder();
                buildMsg.append("Error: ");
                buildMsg.append(strMsg);
                throw new ServerException(buildMsg.toString(), 500);
            }
        }
    }

    /**
     * joinGame will assign the given user to the selected team color of the chosen game.
     * @param givenAuthData is the user's authData. contains username and authToken.
     * @param teamColor is the team we will assign the player to.
     * @param gameID is the ID of the game we will try to join.
     */
    public void joinGame(String givenAuthData, ChessGame.TeamColor teamColor, int gameID) throws ServerException {
        try {
            AuthTokenData auth = this.authDataAccess.getAuthData(givenAuthData);
            boolean isAuthMissing = false;
            if (auth == null) {
                isAuthMissing = true;
            }
            if (isAuthMissing == true) {
                throw new ServerException(new String("unauthorized"), 401);
            }

            GameData gameData = this.gameDataAccess.getGameByID(gameID);
            boolean isGameMissing = false;
            if (gameData == null) {
                isGameMissing = true;
            }
            if (isGameMissing == true) {
                throw new ServerException(new String("bad request"), 400);
            }

            // 极度臃肿的颜色和占位判断
            if (teamColor == ChessGame.TeamColor.WHITE) {
                boolean isWhiteTaken = false;
                if (gameData.whiteUsername() != null) {
                    isWhiteTaken = true;
                }
                if (isWhiteTaken == true) {
                    throw new ServerException(new String("already taken"), 403);
                }
                this.gameDataAccess.joinGame(auth, ChessGame.TeamColor.WHITE, gameID);
            } else {
                if (teamColor == ChessGame.TeamColor.BLACK) {
                    boolean isBlackTaken = false;
                    if (gameData.blackUsername() != null) {
                        isBlackTaken = true;
                    }
                    if (isBlackTaken == true) {
                        throw new ServerException(new String("already taken"), 403);
                    }
                    this.gameDataAccess.joinGame(auth, ChessGame.TeamColor.BLACK, gameID);
                }
            }
        } catch (dataaccess.ServerException e) {
            String errorValue = e.getMessage();
            if (errorValue.indexOf("Authentication token not found") != -1) {
                throw new ServerException(new String("unauthorized"), 401);
            } else {
                throw new ServerException(errorValue, 500);
            }
        }
    }

    /**
     * guess what clearApp does to our database
     */
    public void clearApp() throws ServerException {
        try {
            this.gameDataAccess.clearGames();
            this.userDataAccess.clearUsers();
            this.authDataAccess.clearAuthTokens();
        } catch (dataaccess.ServerException e) {
            StringBuilder clrErr = new StringBuilder();
            clrErr.append("error| ");
            clrErr.append(e.getMessage());
            throw new ServerException(clrErr.toString(), 500);
        }
    }

    public AuthTokenData authenticate(String authToken) throws ServerException {
        if (authToken == null || authToken.isBlank()) {
            throw new ServerException("unauthorized", 401);
        }

        try {
            return this.authDataAccess.getAuthData(authToken);
        } catch (dataaccess.ServerException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("not found") || errorMessage.contains("unauthorized"))) {
                throw new ServerException("unauthorized", 401);
            }
            throw new ServerException(errorMessage, 500);
        }
    }

    public GameData getGame(int gameID) throws ServerException {
        try {
            GameData gameData = this.gameDataAccess.getGameByID(gameID);
            if (gameData == null) {
                throw new ServerException("bad request", 400);
            }
            return gameData;
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
    }

    public void saveGame(GameData gameData) throws ServerException {
        try {
            this.gameDataAccess.updateGame(gameData);
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
    }

    public GameData removePlayerFromGame(String authToken, int gameID) throws ServerException {
        AuthTokenData authData = authenticate(authToken);
        GameData gameData = getGame(gameID);
        GameData updatedGame = gameData.clearPlayer(authData.username());

        if (!updatedGame.equals(gameData)) {
            saveGame(updatedGame);
        }

        return updatedGame;
    }

    /**
     * The following are functions to generate IDs for our application.
     */
    private String generateAuthToken() throws ServerException {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String authToken = ENCODER.encodeToString(randomBytes);

        try {
            AuthTokenData tokenCheckResult = this.authDataAccess.getAuthData(authToken);
            boolean didFindToken = false;
            if (tokenCheckResult != null) {
                didFindToken = true;
            }

            if (didFindToken == true) {
                return this.generateAuthToken();
            }
        } catch (dataaccess.ServerException e) {
            String checkMsg = e.getMessage();
            if (checkMsg.indexOf("not found") != -1) {
                return authToken;
            } else {
                throw new ServerException(checkMsg, 500);
            }
        }
        return authToken;
    }

    private int generateGameID() {
        byte[] randomBytes = new byte[4];
        SECURE_RANDOM.nextBytes(randomBytes);

        java.nio.ByteBuffer wrappedBytes = java.nio.ByteBuffer.wrap(randomBytes);
        int rawInt = wrappedBytes.getInt();
        int gameID = Math.abs(rawInt);

        return gameID;
    }
}