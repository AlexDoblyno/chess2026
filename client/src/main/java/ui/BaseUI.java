package ui;

import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;

import java.util.Scanner;

/**
 * Base UI class that provides common functionality for all UI states
 * <p>
 * References:
 * - Java Scanner documentation: https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html
 * - Java String trim method: https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#trim--
 * - State pattern implementation: https://refactoring.guru/design-patterns/state
 */
public abstract class BaseUI implements UIState {
    protected final ChessClient client;
    public UIStatesEnum state;
    private final Scanner userInputScanner;

    /**
     * Constructor for BaseUI
     *
     * @param chessClient The chess client instance
     */
    public BaseUI(ChessClient chessClient) {
        this.userInputScanner = new Scanner(System.in);
        this.client = chessClient;
    }

    /**
     * Validates that the parameter array has the expected length
     * <p>
     * Reference: Parameter validation patterns in Java
     * https://www.baeldung.com/java-parameter-validation
     *
     * @param parameters             Array of input parameters
     * @param expectedParameterCount Expected number of parameters
     * @throws ResponseException if parameter count doesn't match expected count
     */
    protected void validateParameterLength(String[] parameters, int expectedParameterCount) throws ResponseException {
        if (parameters.length < expectedParameterCount) {
            throw new ResponseException("Parameters missing", 400);
        } else if (parameters.length > expectedParameterCount) {
            throw new ResponseException("Too many parameters given", 400);
        }
    }

    /**
     * Main execution loop that processes user input
     *
     * Reference: Command pattern implementation
     * https://refactoring.guru/design-patterns/command
     *
     * @return The next UI state
     * @throws ResponseException if there's an error processing commands
     */
    public BaseUI run() throws ResponseException {
        boolean shouldContinueRunning = true;

        // Trim inputs for accuracy - removes leading and trailing whitespace
        // Reference: String.trim() method usage
        // https://stackoverflow.com/questions/13432721/what-is-the-use-of-trim-in-java
        while (shouldContinueRunning) {
            String userInput = this.userInputScanner.nextLine().trim();

            try {
                String result = this.handler(userInput);
                if ("quit".equalsIgnoreCase(userInput)) {
                    shouldContinueRunning = false;
                }
                System.out.println(result);
                System.out.print(PromptConstants.PROMPT_SYMBOL);
            } catch (UIStateException stateException) {
                System.out.print(stateException.getMessage());
                System.out.print(PromptConstants.PROMPT_SYMBOL);
                return stateException.getNextState();
            }
        }
        return this;
    }

    /**
     * Constants for UI prompts
     * <p>
     * Reference: Java constants best practices
     * https://www.oracle.com/java/technologies/javase/codeconventions-constants.html
     */
    private static class PromptConstants {
        static final String PROMPT_SYMBOL = "> ";

        // Private constructor to prevent instantiation
        private PromptConstants() {}
    }
}