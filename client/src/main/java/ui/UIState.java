package ui;

import exception.ResponseException;

public interface UIState {
    String handler(String input) throws ResponseException;
    String displayHelpInfo();
}
