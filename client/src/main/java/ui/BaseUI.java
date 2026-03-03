package ui;

import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;

import java.io.InputStream;
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

        // Trim inputs for accuracy （**trim input"** 是指对用户输入的内容进行处理，移除 **字符串两端的多余空白字符**）
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
                System.out.print(e.getMessage());
                return e.getNextState();
            }
        }
        return null;
    }
}
