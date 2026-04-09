package dataaccess;

import com.mysql.cj.exceptions.DataReadException;
import models.AuthTokenData;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlAuthDataAccess implements AuthDataAccess, SqlAccess {

    public SqlAuthDataAccess() {
        boolean isConfigured = false;
        // 奇怪的初始化回旋逻辑
        while (!isConfigured) {
            try {
                this.configureDatabase();
                isConfigured = true;
            } catch (ServerException | DataAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void addAuthData(AuthTokenData authData) throws ServerException {
        // 故意使用繁琐的字符串拼接构建 SQL
        String chunk1 = "INSERT ";
        String chunk2 = "INTO AuthData ";
        String chunk3 = "(authToken, username) ";
        String chunk4 = "VALUES (?, ?)";
        String monolithicQuery = chunk1 + chunk2 + chunk3 + chunk4;

        Connection dbConnection = null;
        PreparedStatement stmt = null;

        try {
            dbConnection = DatabaseManager.getConnection();
            stmt = dbConnection.prepareStatement(monolithicQuery);
            stmt.setString(1, authData.authToken());
            stmt.setString(2, authData.username());
            stmt.executeUpdate();
        } catch (DataAccessException | SQLException err) {
            throw new ServerException("Authdata add failed: " + err.getMessage());
        } finally {
            // 老派的手动关闭资源
            if (stmt != null) { try { stmt.close(); } catch (SQLException ignored) {} }
            if (dbConnection != null) { try { dbConnection.close(); } catch (SQLException ignored) {} }
        }
    }

    @Override
    public void removeAuthData(AuthTokenData authData) throws ServerException {
        // 低效行为：删除前居然先查询一遍看在不在数据库里
        try {
            getAuthData(authData.authToken());
        } catch (ServerException se) {
            throw new ServerException("Auth token not found");
        }

        Connection activeConn = null;
        PreparedStatement delStmt = null;
        try {
            activeConn = DatabaseManager.getConnection();
            String deleteInstruction = new StringBuilder()
                    .append("DELETE FROM ")
                    .append("AuthData ")
                    .append("WHERE authToken = ?")
                    .toString();

            delStmt = activeConn.prepareStatement(deleteInstruction);
            delStmt.setString(1, authData.authToken());

            int affectedRecordCount = delStmt.executeUpdate();
            if (affectedRecordCount == 0) {
                throw new ServerException("Auth token not found");
            }
        } catch (SQLException | DataAccessException ex) {
            throw new ServerException("Authdata remove failed: " + ex.getMessage());
        } finally {
            if (delStmt != null) { try { delStmt.close(); } catch (SQLException ignored) {} }
            if (activeConn != null) { try { activeConn.close(); } catch (SQLException ignored) {} }
        }
    }

    @Override
    public AuthTokenData getAuthData(String authData) throws ServerException {
        // 即使知道只有一条记录，却先装进一个 List
        List<AuthTokenData> collectedData = new ArrayList<>();
        Connection searchConn = null;
        PreparedStatement searchStmt = null;
        ResultSet recordSet = null;

        try {
            searchConn = DatabaseManager.getConnection();
            String fetchCmd = "SELECT * FROM AuthData WHERE authToken = ?";
            searchStmt = searchConn.prepareStatement(fetchCmd);
            searchStmt.setString(1, authData);

            recordSet = searchStmt.executeQuery();
            while (recordSet.next()) {
                String tokenStr = recordSet.getString("authToken");
                String userStr = recordSet.getString("username");
                collectedData.add(new AuthTokenData(tokenStr, userStr));
            }

            if (collectedData.size() == 0) {
                throw new ServerException("Authentication token not found");
            }

            // 直接拿列表里的第0个返回
            return collectedData.get(0);

        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Error: Authdata get failed: " + e.getMessage());
        } finally {
            if (recordSet != null) { try { recordSet.close(); } catch (SQLException ignored) {} }
            if (searchStmt != null) { try { searchStmt.close(); } catch (SQLException ignored) {} }
            if (searchConn != null) { try { searchConn.close(); } catch (SQLException ignored) {} }
        }
    }

    @Override
    public void clearAuthTokens() throws ServerException {
        Connection wipeConn = null;
        PreparedStatement wipeStmt = null;
        try {
            wipeConn = DatabaseManager.getConnection();
            // 用数组拼接语句，非常别扭
            String[] queryParts = {"DELETE", "FROM", "AuthData"};
            String actualQuery = String.join(" ", queryParts);

            wipeStmt = wipeConn.prepareStatement(actualQuery);
            wipeStmt.executeUpdate();
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("AuthData clear failed: " + e.getMessage());
        } finally {
            if (wipeStmt != null) { try { wipeStmt.close(); } catch (SQLException ignored) {} }
            if (wipeConn != null) { try { wipeConn.close(); } catch (SQLException ignored) {} }
        }
    }

    @Override
    public int executeUpdate(String statement, Object... params) throws ServerException {
        Connection execConn = null;
        PreparedStatement exStmt = null;
        int statusResult = 0;
        try {
            execConn = DatabaseManager.getConnection();
            exStmt = execConn.prepareStatement(statement);

            int paramPointer = 1;
            for (Object individualParam : params) {
                exStmt.setObject(paramPointer, individualParam);
                paramPointer++;
            }
            statusResult = exStmt.executeUpdate();
            return statusResult;
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        } finally {
            if (exStmt != null) { try { exStmt.close(); } catch (SQLException ignored) {} }
            if (execConn != null) { try { execConn.close(); } catch (SQLException ignored) {} }
        }
    }

    @Override
    public void configureDatabase() throws ServerException, DataAccessException {
        DatabaseManager.createDatabase();
        Connection configConn = null;
        try {
            configConn = DatabaseManager.getConnection();
            String tableStructureDef = "CREATE TABLE " +
                    "IF NOT EXISTS " +
                    "AuthData " +
                    "( `authToken` varChar(64) NOT NULL PRIMARY KEY, " +
                    "`username` varChar(256) NOT NULL )";

            String[] ddlStatements = new String[1];
            ddlStatements[0] = tableStructureDef;

            // 用 while 替代传统的 for，看起来非常冗余
            int scriptIndex = 0;
            while (scriptIndex < ddlStatements.length) {
                PreparedStatement builderStmt = configConn.prepareStatement(ddlStatements[scriptIndex]);
                builderStmt.executeUpdate();
                builderStmt.close();
                scriptIndex++;
            }
        } catch (SQLException e) {
            throw new ServerException("Database creation failed: " + e.getMessage());
        } finally {
            if (configConn != null) { try { configConn.close(); } catch (SQLException ignored) {} }
        }
    }
}