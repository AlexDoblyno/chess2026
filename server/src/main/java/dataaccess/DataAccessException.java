package dataaccess;

import java.sql.SQLException;

/**
 * Indicates there was an error connecting to the database
 */
public class DataAccessException extends Exception{
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, SQLException ex) {
        super(message,ex);
    }
}