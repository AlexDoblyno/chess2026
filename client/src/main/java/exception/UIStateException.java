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

//`UIStateException` 是自定义的异常类，主要用于在用户界面（UI）的不同状态或者界面间进行**状态切换**。本质上，它是一种“非常规”方式实现的状态管理机制，用异常处理来指引 UI 层状态的跳转。
//- **传统方式**：通常状态跳转是通过明确的返回值或状态机来实现，例如通过返回一个标志值来决定下一步的 UI 界面。
//- **当前实现**：程序通过抛出 `UIStateException`，将其捕获后自动完成状态切换，避免手动去管理复杂的状态返回逻辑。