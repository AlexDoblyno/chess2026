package client;

import chess.ChessGame;
import exception.ResponseException;
import models.AuthTokenData;
import models.GameData;
import models.UserData;
import ui.*;


import java.util.Collection;

import static chess.ChessGame.TeamColor.OBSERVE;

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
// parameters only need to be a game name. AuthToken is stored in the DataCache.
    public String createGame(String... parameters) throws ResponseException {

        int gameID = server.createGame(dataCache.getAuthToken(), parameters[0]);

        // 【修改这里】：不打印 gameID，只打印成功的提示和游戏房间名
        return "Successfully created game room: " + parameters[0];
    }

    // parameters[1] is the team color, and parameters[2] is the gameID
// 升级版 joinGame：防崩溃、智能识别参数顺序、友好提示已满房间
    public String joinGame(String... parameters) {
        try {
            if (parameters.length < 2) {
                return "Error: Expected format is 'join [team color] [game ID]', e.g., 'join white 1'";
            }

            String colorStr;
            String idStr;

            // 智能判断参数顺序：允许 "join white 1" 也可以 "join 1 white"
            if (parameters[0].matches("\\d+")) { // 如果第一个参数是纯数字
                idStr = parameters[0];
                colorStr = parameters[1];
            } else {
                colorStr = parameters[0];
                idStr = parameters[1];
            }

            ChessGame.TeamColor teamColor = getTeamColor(new String[]{colorStr});
            if (teamColor == null) {
                return "Error: Invalid team color. Please choose 'white' or 'black'.";
            }

            int gameIndex;
            try {
                gameIndex = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                return "Error: Invalid game ID number.";
            }

            Collection<GameData> gameList = server.listGame(dataCache.getAuthToken());
            dataCache.setGameCache(gameList);

            // 检查房间是否存在
            GameData gameData = dataCache.getGameByIndex(gameIndex);
            if (gameData == null) {
                return "Error: Game not found. Please check your game list.";
            }

            // 【关键】：尝试加入游戏。如果服务器报错（例如座位有人了），在这里拦住它！
            try {
                server.joinGame(dataCache.getAuthToken(), teamColor, gameData.gameID());
            } catch (ResponseException e) {
                // 如果抛出 already taken 等异常，返回红色的友好提示，绝不崩溃！
                return EscapeSequences.SET_TEXT_COLOR_RED + "Failed to join: " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR;
            }

            // Set the gameboard drawer
            drawBoard.setChessGame(gameData.game());
            drawBoard.setPerspective(teamColor);

            StringBuilder resultString = new StringBuilder(drawBoard.drawBoardString());
            resultString.append(String.format("Game - %s\nWhite - %s%s%s\nBlack - %s%s%s",
                    EscapeSequences.SET_TEXT_COLOR_BLUE, gameData.gameName(), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_TEXT_COLOR_WHITE, gameData.whiteUsername(), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_BG_COLOR_DARK_GREY, gameData.blackUsername(), EscapeSequences.RESET_TEXT_COLOR));
            return resultString.toString();

        } catch (Exception e) {
            return "An unexpected error occurred: " + e.getMessage();
        }
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
        int gameindex = Integer.parseInt(parameters[0]);
        Collection<GameData> gameList = server.listGame(dataCache.getAuthToken());
        dataCache.setGameCache(gameList);
        GameData gameData = dataCache.getGameByIndex(gameindex);

        server.joinGame(dataCache.getAuthToken(), OBSERVE, gameData.gameID());

        // Set the gameboard drawer
        drawBoard.setChessGame(gameData.game());
        return drawBoard.drawBoardString();
    }
    public String quitGame() throws ResponseException {
        //server
        return "Quit";
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
