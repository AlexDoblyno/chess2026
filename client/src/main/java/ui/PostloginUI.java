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
            // 注意这里：改成了 return，以便能返回报错字符串
            case "join" -> {
                return join(tokens);
            }
            case "observe" -> {
                return observe(tokens);
            }
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

    // 把 void 改成了 String
    private String join(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 3);
        String joinTeam;
        String gameIDStr;

        // 智能识别参数顺序：支持 "join white 1" 和 "join 1 white"
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
            return "Error: Invalid game ID number.\n";
        }

        if (!joinTeam.equalsIgnoreCase("WHITE") && !joinTeam.equalsIgnoreCase("BLACK")) {
            return "Error: Invalid team color. Please choose 'white' or 'black'.\n";
        }

        // 调用客户端逻辑去尝试加入游戏
        String result = client.joinGame(joinTeam, gameIDStr);

        // 🚨 【核心修复点】：如果返回值里包含了 Failed 或者 Error，说明没加进去。
        // 我们直接返回报错信息，停止往下走，不切入 GameUI！
        if (result.contains("Failed") || result.contains("Error")) {
            return result + "\n\n"; // 多加两个回车，防止跟下一个输入框粘连
        }

        ChessGame.TeamColor teamColor = (joinTeam.equalsIgnoreCase("WHITE") ?
                ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK);

        GameData gameData = client.getDataCache().getGameByIndex(gameID);
        if (gameData == null) {
            return "Error: Game not found. Please check your game list.\n";
        }

        ChessboardDrawer drawer = new ChessboardDrawer(gameData.game(), teamColor);
        GameUI gameUI = new GameUI(client, drawer, true);

        // 如果上面一切顺利，才会抛出异常跳转进游戏房间，并打印棋盘
        throw new UIStateException(gameUI, result);
    }

    private String observe(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        int gameID = Integer.parseInt(tokens[1]);
        String result = client.observeGame(tokens[1]);

        // 观战也加上失败拦截
        if (result.contains("Failed") || result.contains("Error")) {
            return result + "\n\n";
        }

        GameData gameData = client.getDataCache().getGameByIndex(gameID);
        if (gameData == null) {
            return "Error: Game not found.\n";
        }

        ChessboardDrawer drawer = new ChessboardDrawer(gameData.game(), ChessGame.TeamColor.WHITE);
        GameUI gameUI = new GameUI(client, drawer, false);

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