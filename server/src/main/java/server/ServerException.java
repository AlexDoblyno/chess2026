package server;

/**
 * Separate class to define a server exception, allows for the status code and message to vary rather than having
 * multiple unnecessary tiny exception classes.
 */
public class ServerException extends Exception {
    private final int statusCode;

    public ServerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}