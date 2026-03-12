package client;

import chess.ChessGame;
import exception.ResponseException;
import Models.AuthTokenData;
import Models.GameData;
import Models.UserData;
import ui.*;


import java.util.Collection;

public class ChessClient {
    private final ServerFacade server;
    private final String serverURL;
    private final DataCache dataCache;
    private ChessboardDrawer drawBoard;

    // Set up client server connection
    public ChessClient(String serverURL) {
        this.serverURL = serverURL;
        server = new ServerFacade(this.serverURL);
        dataCache = new DataCache();
        drawBoard = new ChessboardDrawer();
    }

    public String register(String... parameters) throws ResponseException {

        UserData user = new UserData(parameters[0], parameters[1], parameters[2]);

        // Set auth token in cached data object
        AuthTokenData authTokenData = server.registerUser(user);
        dataCache.setAuthToken(authTokenData.authToken());

        return user.username() + " has been successfully registered!";
    }

    public String login(String... parameters) throws ResponseException {
        AuthTokenData authTokenData = server.loginUser(parameters[0], parameters[1]);
        dataCache.setAuthToken(authTokenData.authToken());
        return parameters[1] + " has been successfully logged in!";
    }

    public String listGames() throws ResponseException {

        String authToken = dataCache.getAuthToken();
        Collection<GameData> gameList = server.listGame(authToken);
        dataCache.setGameCache(gameList);

        // Create string display result using StringBuilder
        StringBuilder resultString = new StringBuilder();
        resultString.append(EscapeSequences.ERASE_SCREEN);
        resultString.append(EscapeSequences.SET_TEXT_BOLD).append("\n --< Active Games >--\n")
                .append(EscapeSequences.RESET_TEXT_BOLD_FAINT);

        int index = 1;
        for (GameData game : gameList) {
            // Placeholders to insert necessary information, unfortunately creates a really annoying run-on string.
            // Might want to reimplement later for more conciseness
            resultString.append(String.format("%s%d.%s %s%s%s [ White: %s%s%s | Black: %s%s%s ]\n",
                    EscapeSequences.SET_TEXT_COLOR_BLUE, index++, EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_TEXT_BOLD, game.gameName(), EscapeSequences.RESET_TEXT_BOLD_FAINT,
                    EscapeSequences.SET_TEXT_COLOR_WHITE, game.whiteUsername(), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_BG_COLOR_DARK_GREY, game.blackUsername(), EscapeSequences.RESET_TEXT_COLOR));
        }
        return resultString.toString();
    }

    // parameters only need to be a game name. AuthToken is stored in the DataCache.
    public String createGame(String... parameters) throws ResponseException {

        int gameID = server.createGame(dataCache.getAuthToken(), parameters[0]);

        // Create string display result using StringBuilder
        StringBuilder resultString = new StringBuilder();
        return "Successful game creation with ID " + gameID;
    }

    // parameters[1] is the team color, and parameters[2] is the gameID
    public String joinGame(String... parameters) throws ResponseException {
        ChessGame.TeamColor teamColor = getTeamColor(parameters);
        // Find the gameID based on our cacheData number system
        GameData gameData = dataCache.getGameByIndex(Integer.parseInt(parameters[1]));

        server.joinGame(dataCache.getAuthToken(), teamColor, gameData.gameID());

        // Set the gameboard drawer
        drawBoard.setChessGame(gameData.game());
        drawBoard.setPerspective(teamColor);

        StringBuilder resultString = new StringBuilder();
        resultString.append(String.format("Game - %s\nWhite - %s%s%s\nBlack - %s%s%s",
                EscapeSequences.SET_TEXT_COLOR_BLUE, gameData.gameName(), EscapeSequences.RESET_TEXT_COLOR,
                EscapeSequences.SET_TEXT_COLOR_WHITE, gameData.whiteUsername(), EscapeSequences.RESET_TEXT_COLOR,
                EscapeSequences.SET_BG_COLOR_DARK_GREY, gameData.blackUsername(), EscapeSequences.RESET_TEXT_COLOR));
        return resultString.toString();
    }

    private static ChessGame.TeamColor getTeamColor(String[] parameters) {
        // Determine team color
        ChessGame.TeamColor teamColor;
        if (parameters[0].contains("white") || parameters[0].contains("WHITE")) {
            teamColor = ChessGame.TeamColor.WHITE;
        }
        else if (parameters[0].contains("black") || parameters[0].contains("BLACK")) {
            teamColor = ChessGame.TeamColor.BLACK;
        }
        else {
            teamColor = null;
        }
        return teamColor;
    }

    // parameters[1] is the gameID
    public String observeGame(String... parameters) throws ResponseException {
        GameData gameData = dataCache.getGameByIndex(Integer.parseInt(parameters[1]));

        server.joinGame(dataCache.getAuthToken(), null, gameData.gameID());

        // Set the gameboard drawer
        drawBoard.setChessGame(gameData.game());
        return drawBoard.drawBoardString();
    }

    public String logout() throws ResponseException {
        server.logoutUser(dataCache.getAuthToken());
        dataCache.setAuthToken(null);
        return "Logout successful!";
    }

    public DataCache getDataCache() {
        return dataCache;
    }

}
