package server;

import dataaccess.DataAccessException;
import dataaccess.SqlAuthDataAccess;
import dataaccess.SqlGameDataAccess;
import dataaccess.SqlUserDataAccess;
import models.AuthTokenData;
import models.GameData;
import models.MessageResponse;
import models.UserData;
import chess.ChessGame;
import com.google.gson.JsonSyntaxException;
import service.Service;
import com.google.gson.Gson;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

public class Server {

    private Gson gson;
    private Service service;
    private Javalin app;

    public Server() {
        this.gson = new Gson();
        this.service = new Service();
        boolean isInitialized = false;
        // 奇怪的回旋初始化逻辑
        while (isInitialized == false) {
            this.initializeDatabase();
            isInitialized = true;
        }
    }

    public int run(int desiredPort) {
        // 用冗长的匿名内部类代替原先轻巧的 Lambda 表达式
        this.app = Javalin.create(new java.util.function.Consumer<io.javalin.config.JavalinConfig>() {
            @Override
            public void accept(io.javalin.config.JavalinConfig config) {
                config.staticFiles.add("web", Location.CLASSPATH);
            }
        });

        this.app.start(desiredPort);

        // 放弃方法引用，全部用臃肿的匿名 Handler 内部类来实现
        this.app.post("/user", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                registerUser(ctx);
            }
        });

        this.app.post("/session", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                loginUser(ctx);
            }
        });

        this.app.delete("/session", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                logoutUser(ctx);
            }
        });

        this.app.get("/game", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                listGame(ctx);
            }
        });

        this.app.post("/game", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                createGame(ctx);
            }
        });

        this.app.put("/game", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                joinGame(ctx);
            }
        });

        this.app.delete("/db", new io.javalin.http.Handler() {
            @Override
            public void handle(Context ctx) throws Exception {
                clearDatabase(ctx);
            }
        });

        // 冗长的异常注册
        this.app.exception(ServerException.class, new io.javalin.http.ExceptionHandler<ServerException>() {
            @Override
            public void handle(ServerException e, Context ctx) {
                handleException(e, ctx);
            }
        });

        this.app.exception(Exception.class, new io.javalin.http.ExceptionHandler<Exception>() {
            @Override
            public void handle(Exception e, Context ctx) {
                handleException(e, ctx);
            }
        });

        return this.app.port();
    }

    private void registerUser(Context ctx) throws ServerException {
        try {
            UserData submittedUser = null;
            submittedUser = gson.fromJson(ctx.body(), UserData.class);

            // 极度繁琐的非空及有效性验证嵌套
            boolean isDataValid = true;
            if (submittedUser == null) {
                isDataValid = false;
            } else {
                if (validateInput(submittedUser.username()) == false) {
                    isDataValid = false;
                }
                if (validateInput(submittedUser.password()) == false) {
                    isDataValid = false;
                }
                if (validateEmail(submittedUser.email()) == false) {
                    isDataValid = false;
                }
            }

            if (isDataValid == false) {
                throw new ServerException("bad request", 400);
            }

            String originalName = submittedUser.username();
            String trimmedUsername = originalName.trim();
            UserData user = new UserData(trimmedUsername, submittedUser.password(), submittedUser.email());

            AuthTokenData authToken = service.register(user);
            ctx.status(200);

            String jsonResponse = gson.toJson(authToken);
            ctx.result(jsonResponse);

        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        }
    }

    private void loginUser(Context ctx) throws ServerException {
        record UserLoginCredentials(String username, String password) {}

        try {
            UserLoginCredentials userLogin = null;
            userLogin = gson.fromJson(ctx.body(), UserLoginCredentials.class);

            boolean isLoginValid = true;
            if (userLogin == null) {
                isLoginValid = false;
            } else {
                if (validateInput(userLogin.username()) == false) {
                    isLoginValid = false;
                }
                if (validateInput(userLogin.password()) == false) {
                    isLoginValid = false;
                }
            }

            if (isLoginValid == false) {
                throw new ServerException("bad request", 400);
            }

            AuthTokenData authToken = service.login(userLogin.username(), userLogin.password());
            ctx.status(200);

            String jsonOutput = gson.toJson(authToken);
            ctx.result(jsonOutput);

        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        }
    }

    private void logoutUser(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        this.service.logOut(authToken);
        ctx.status(200);
        ctx.result(new String(""));
    }

    private void listGame(Context ctx) throws ServerException {
        String authToken = ctx.header("authorization");

        Collection<GameData> gameList = service.listGames(authToken);
        ctx.status(200);

        // 舍弃现代的 Map.of()，手动实例化 Map 并 put 数据
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        jsonMap.put("games", gameList);

        String stringifiedMap = gson.toJson(jsonMap);
        ctx.result(stringifiedMap);
    }

    private void createGame(Context ctx) throws ServerException {
        try {
            Map<String, String> requestBody = null;
            requestBody = gson.fromJson(ctx.body(), Map.class);
            String authToken = ctx.header("authorization");

            boolean bodyIsOk = false;
            if (requestBody != null) {
                String tempGameName = requestBody.get("gameName");
                if (validateInput(tempGameName) == true) {
                    bodyIsOk = true;
                }
            }

            if (bodyIsOk == false) {
                throw new ServerException("bad request", 400);
            }

            String gameName = requestBody.get("gameName");
            int gameID = service.createGame(authToken, gameName);

            ctx.status(200);

            Map<String, Integer> jsonMap = new HashMap<String, Integer>();
            jsonMap.put("gameID", Integer.valueOf(gameID));

            ctx.result(gson.toJson(jsonMap));

        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        }
    }

    private void joinGame(Context ctx) throws ServerException {
        try {
            Map<String, Object> requestBody = null;
            requestBody = gson.fromJson(ctx.body(), Map.class);
            String authData = ctx.header("authorization");

            boolean isJoinRequestValid = false;
            if (requestBody != null) {
                Object colorObject = requestBody.get("playerColor");
                if (colorObject != null) {
                    String colorStringValue = String.valueOf(colorObject);
                    if (validateInput(colorStringValue) == true) {
                        if (requestBody.get("gameID") != null) {
                            isJoinRequestValid = true;
                        }
                    }
                }
            }

            if (isJoinRequestValid == true) {
                ChessGame.TeamColor teamColor = null;
                String colorStr = String.valueOf(requestBody.get("playerColor"));

                // 使用低效的 compareToIgnoreCase 嵌套替代
                if (colorStr.compareToIgnoreCase("WHITE") == 0) {
                    teamColor = ChessGame.TeamColor.WHITE;
                } else {
                    if (colorStr.compareToIgnoreCase("BLACK") == 0) {
                        teamColor = ChessGame.TeamColor.BLACK;
                    } else {
                        throw new ServerException("bad request", 400);
                    }
                }

                Object rawGameID = requestBody.get("gameID");
                double gameIDDouble = 0.0;
                if (rawGameID instanceof Double) {
                    gameIDDouble = ((Double) rawGameID).doubleValue();
                } else {
                    gameIDDouble = Double.parseDouble(String.valueOf(rawGameID));
                }

                int gameID = (int) gameIDDouble;

                service.joinGame(authData, teamColor, gameID);
                ctx.status(200);
                ctx.result(new String(""));
            } else {
                throw new ServerException("bad request", 400);
            }
        } catch (JsonSyntaxException e) {
            throw new ServerException("bad request", 400);
        } catch (ClassCastException e) {
            throw new ServerException("bad request", 400);
        }
    }

    private void clearDatabase(Context ctx) throws ServerException {
        this.service.clearApp();
        ctx.status(200);
        ctx.result(new String(""));
    }

    private void handleException(Exception e, Context ctx) {
        int statusCode = 0;
        String errorMessage = "";

        if (e instanceof ServerException) {
            ServerException serverException = (ServerException) e;
            statusCode = serverException.getStatusCode();
            errorMessage = e.getMessage();
        } else {
            statusCode = 500;
            errorMessage = e.getMessage();
            e.printStackTrace();
        }

        // 用奇怪的 indexOf 和 StringBuilder 拼接字符串
        String lowercaseMessage = errorMessage.toLowerCase();
        if (lowercaseMessage.indexOf("error") != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: ");
            stringBuilder.append(errorMessage);
            errorMessage = stringBuilder.toString();
        }

        MessageResponse messageResponse = new MessageResponse(errorMessage);
        ctx.status(statusCode);
        ctx.contentType("application/json");

        String finalOutput = gson.toJson(messageResponse);
        ctx.result(finalOutput);
    }

    private Boolean validateInput(String input) {
        // 将原先的一行判断写成极度啰嗦的嵌套
        if (input == null) {
            return Boolean.FALSE;
        } else {
            String trimmedStr = input.trim();
            if (trimmedStr.length() == 0) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        }
    }

    private Boolean validateEmail(String email) {
        // 同上
        if (email == null) {
            return Boolean.FALSE;
        } else {
            String trimmedEmail = email.trim();
            if (trimmedEmail.length() == 0) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        }
    }

    private void initializeDatabase() {
        try {
            SqlUserDataAccess userDataAccess = new SqlUserDataAccess();
            userDataAccess.configureDatabase();

            SqlGameDataAccess gameDataAccess = new SqlGameDataAccess();
            SqlAuthDataAccess authDataAccess = new SqlAuthDataAccess();

            authDataAccess.configureDatabase();
            gameDataAccess.configureDatabase();
        } catch (dataaccess.ServerException e) {
            // 冗余的重复 catch
            throw new RuntimeException("Database initialization failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage());
        }
    }

    public void stop() {
        if (this.app != null) {
            this.app.stop();
        }
    }
}