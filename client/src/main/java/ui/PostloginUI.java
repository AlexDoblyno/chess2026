package ui;

import chess.ChessGame;
import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;
import models.GameData;

public class PostloginUI extends BaseUI {

    public PostloginUI(ChessClient client) {
        super(client);
        state = UIStatesEnum.POSTLOGINUI;
    }

    @Override
    public String handler(String input) throws ResponseException {
        String[] tokens = input.split(" ");
        switch (tokens[0].toLowerCase()) {
            case "list" -> {
                return list();
            }
            case "create" -> {
                return create(tokens);
            }
            case "join" -> join(tokens);
            case "observe" -> observe(tokens);
            case "logout" -> logoutUser();
            default -> {
                return displayHelpInfo();
            }
        };
        return null;
    }

    private String list() throws ResponseException {
        return client.listGames();
    }

    private String create(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        String gameName = tokens[1];
        return client.createGame(gameName);
    }

    private void join(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 3);
        String joinTeam = tokens[1];
        String gameID = tokens[2];
        String result = client.joinGame(joinTeam,gameID);
        ChessGame.TeamColor teamColor = (joinTeam.equalsIgnoreCase("WHITE") ?
                ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK);

        GameData gameData = client.getDataCache().getGameByIndex(Integer.parseInt(gameID));
        ChessboardDrawer drawer = new ChessboardDrawer(gameData.game(), teamColor);
        GameUI gameUI = new GameUI(client, drawer, true);

        throw new UIStateException(gameUI, result);
    }

    private void observe(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        int gameID = Integer.parseInt(tokens[1]);
        String result = client.observeGame(tokens[1]);

        GameData gameData = client.getDataCache().getGameByIndex(gameID);
        ChessboardDrawer drawer = new ChessboardDrawer(gameData.game(), ChessGame.TeamColor.WHITE);
        GameUI gameUI = new GameUI(client, drawer, false);

        //System.out.println(result);

        throw new UIStateException(gameUI, result);
    }

    private void logoutUser() throws ResponseException {
        String result = client.logout();
        client.getDataCache().setAuthToken(null);
        throw new UIStateException(new PreloginUI(client), result);
    }

    @Override
    public String displayHelpInfo() {
        return """
                --- HELP ---
                Type a command to get the corresponding action.
                - list                        | List all existing games.
                - create [game name]          | Create a new game with your specified name.
                - join [team color] [game ID] | Join an existing game with the specified game ID.
                - observe [game ID]           | Observe a specified game as a spectator.
                - logout                      | Logout the current user.
                - help                        | Display this help menu.
                """;
    }
}
