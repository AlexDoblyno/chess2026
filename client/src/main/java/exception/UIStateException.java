package exception;

import ui.BaseUI;

/**
 * UIStateException is used to help us transition between UI States without closing the scanner or any of the previous
 * states completely. This will allow seamless transitions between the nested states of prelogin, postlogin, and game.
 * It allows me to throw an exception to be caught by the run method, bypassing any complicated return trees.
 * Theoretically this SHOULD work. It may be poor practice? It makes sense in my head.
 */
public class UIStateException extends ResponseException {
  private final BaseUI nextState;
  private final String message;

  public UIStateException(BaseUI nextState, String message) {
    super("Transitioning UIState", 200);
    this.nextState = nextState;
    this.message = message;
  }

  public BaseUI getNextState() {
    return nextState;
  }

  public String getMessage() {
    return message;
  }
}
