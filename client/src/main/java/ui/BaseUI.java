package ui;

import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;

import java.util.Scanner;

public abstract class BaseUI implements UIState{
    protected final ChessClient client;
    public UIStatesEnum state;
    private Scanner scanner;

    public BaseUI(ChessClient client) {
        scanner = new Scanner(System.in);
        this.client = client;
    }

    public void validateParameterLength(String[] params, int expectedLength) throws ResponseException {
        if (params.length < expectedLength) {
            throw new ResponseException("Parameters missing", 400);
        }
        else if (params.length > expectedLength) {
            throw new ResponseException("Too many parameters given", 400);
        }
    }

    public BaseUI run() throws ResponseException {
        boolean keepRunning = true;

        // Trim inputs for accuracy
        while (keepRunning) {
            String input = scanner.nextLine().trim();

            try {
                String result = handler(input);
                if ("quit".equalsIgnoreCase(input)) {
                    keepRunning = false;
                }
                System.out.println(result);
                System.out.print("> ");
            } catch (UIStateException e) {
                return e.getNextState();
            }
        }
        return null;
    }
}
