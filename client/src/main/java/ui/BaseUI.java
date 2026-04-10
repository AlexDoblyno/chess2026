package ui;

import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;

import java.util.Scanner;

public abstract class BaseUI implements UIState{
    protected final ChessClient client;
    public UIStatesEnum state;
    private final Scanner scanner;

    public BaseUI(ChessClient client) {
        scanner = new Scanner(System.in);
        this.client = client;
    }

    public void validateParameterLength(String[] params, int expectedLength) throws ResponseException {
        if (params.length < expectedLength)
            throw new ResponseException("Not enough parameters.", 400);
        if (params.length > expectedLength)
            throw new ResponseException("Too many parameters.", 400);
    }

    protected String[] tokenizeInput(String input) {
        return input.trim().split("\\s+");
    }

    protected String formatError(String message) {
        return EscapeSequences.SET_TEXT_COLOR_RED + message + EscapeSequences.RESET_TEXT_COLOR;
    }

    public BaseUI run() {
        boolean keepRunning = true;
        System.out.print("> ");

        while (keepRunning) {
            if (!scanner.hasNextLine())
                return null;

            String input = scanner.nextLine().trim();
            if (input.isBlank()) {
                System.out.print("> ");
                continue;
            }

            try {
                String result = handler(input);
                if ("quit".equalsIgnoreCase(input))
                    keepRunning = false;

                if (result != null && !result.isBlank())
                    System.out.println(result);
                if (keepRunning)
                    System.out.print("> ");
            } catch (UIStateException e) {
                if (e.getMessage() != null && !e.getMessage().isBlank())
                    System.out.println(e.getMessage());
                return e.getNextState();
            } catch (ResponseException e) {
                System.out.println(formatError(e.getMessage()));
                System.out.print("> ");
            }
        }
        return null;
    }
}
