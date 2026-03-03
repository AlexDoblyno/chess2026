package ui;

import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;

public class PreloginUI extends BaseUI {
    public PreloginUI(ChessClient client) {
        super(client);
        state = UIStatesEnum.PRELOGINUI;
    }

    @Override
    public String handler(String input) throws ResponseException {
        String[] tokens = input.split(" ");
        switch (tokens[0].toLowerCase()) {
            case "register" ->  {
                handleRegister(tokens);
            }
            case "login" -> {
                handleLogin(tokens);
            }
            case "quit" -> {return "quit";}
            default -> {return displayHelpInfo();}
        };
        return null;
    }

    /**
     * register will take the tokens inputted, call the client's register function with them, and then throw a new
     * UIStateException. This UIStateException will then introduce the next UIState.
     * @param tokens inputted parameters
     * @throws ResponseException errors if they come up. Otherwise,
     * @throws UIStateException as the success state, which will keep the scanner object open and seamlessly continue
     * to the next UI state.
     */
    private void handleRegister(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 4);
        String registerResult = client.register(tokens[1], tokens[2], tokens[3]);
        System.out.println(registerResult);

        // Set welcome string
        System.out.println(buildWelcomeMessage(tokens));

        throw new UIStateException(new PostloginUI(client), "\n");
    }

    private String buildWelcomeMessage(String[] tokens) {
        return EscapeSequences.ERASE_SCREEN + EscapeSequences.SET_TEXT_BOLD + EscapeSequences.SET_BG_COLOR_WHITE +
                EscapeSequences.SET_TEXT_COLOR_BLACK + "Welcome, player " + tokens[1] +
                "!\n" + EscapeSequences.RESET_TEXT_BOLD_FAINT + EscapeSequences.SET_TEXT_COLOR_DARK_GREY +
                "Create or join a game! Type 'help' to see a list of commands." + EscapeSequences.RESET_BG_COLOR +
                EscapeSequences.RESET_TEXT_COLOR;
    }

    /**
     * login acts much the same as register. It will take the tokens inputted, call the client's login function with them,
     * and then throw a new UIStateException. This UIStateException will then introduce the next UIState.
     * @param tokens inputted parameters
     * @throws ResponseException errors if they come up. Otherwise,
     * @throws UIStateException as the success state, which will keep the scanner object open and seamlessly continue
     * to the next UI state.
     */
    private void handleLogin(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 3);
        String loginResult = client.login(tokens[1], tokens[2]);

        System.out.println(loginResult);
        System.out.println(buildWelcomeMessage(tokens));
        throw new UIStateException(new PostloginUI(client), "\n");
    }

    @Override
    public String displayHelpInfo() {
        return """
                --- HELP ---
                Type a command to get the corresponding action.
                - register [username] [password] [email] | Register a new user.
                - login [username] [password]            | Login an existing user.
                - quit                                   | Quit the application.
                - help                                   | Display this help menu.
                """;
    }
}