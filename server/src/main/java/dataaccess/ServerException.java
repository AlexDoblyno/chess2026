package dataaccess;

/**
 * Indicates there was an error connecting to the database
 */
public class ServerException extends Exception{
    public ServerException(String message) {
        super(message);
    }
}