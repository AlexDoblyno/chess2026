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
        String[] tokens = tokenizeInput(input);
        switch (tokens[0].toLowerCase()) {
            case "list" -> {
                return list();
            }
            case "create" -> {
                return create(input);
            }
            case "join" -> {
                join(tokens);
                return null;
            }
            case "observe" -> {
                observe(tokens);
                return null;
            }
            case "logout" -> logoutUser();
            default -> {
                return displayHelpInfo();
            }
        }
        return null;
    }

    private String list() throws ResponseException {
        return client.listGames();
    }

    private String create(String input) throws ResponseException {
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank())
            throw new ResponseException("Create requires a game name.", 400);
        return client.createGame(parts[1].trim());
    }

    private void join(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 3);
        String joinTeam;
        String gameIDStr;

        if (tokens[1].matches("\\d+")) {
            gameIDStr = tokens[1];
            joinTeam = tokens[2];
        } else {
            joinTeam = tokens[1];
            gameIDStr = tokens[2];
        }

        int gameID;
        try {
            gameID = Integer.parseInt(gameIDStr);
        } catch (NumberFormatException e) {
            throw new ResponseException("Game number must be a number.", 400);
        }

        ChessGame.TeamColor teamColor = parseTeamColor(joinTeam);
        if (teamColor == null)
            throw new ResponseException("Choose 'white' or 'black' when joining a game.", 400);

        GameData gameData = client.joinGame(teamColor, gameID);
        GameUI gameUI = new GameUI(client, gameData, teamColor, true);
        throw new UIStateException(gameUI, gameUI.initialScreen());
    }

    private void observe(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        int gameID;
        try {
            gameID = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            throw new ResponseException("Game number must be a number.", 400);
        }

        GameData gameData = client.observeGame(gameID);
        GameUI gameUI = new GameUI(client, gameData, ChessGame.TeamColor.WHITE, false);
        throw new UIStateException(gameUI, gameUI.initialScreen());
    }

    private void logoutUser() throws ResponseException {
        String result = client.logout();
        client.getDataCache().setAuthToken(null);
        throw new UIStateException(new PreloginUI(client), result);
    }

    private ChessGame.TeamColor parseTeamColor(String token) {
        if (token.equalsIgnoreCase("white"))
            return ChessGame.TeamColor.WHITE;
        if (token.equalsIgnoreCase("black"))
            return ChessGame.TeamColor.BLACK;
        return null;
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