package dataaccess;

public interface SqlAccess {
    int executeUpdate(String statement, Object... params) throws ServerException;
    void configureDatabase() throws ServerException, DataAccessException;
}