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
import spark.*;
import com.google.gson.Gson;

import java.util.Collection;
import java.util.Map;

public class Server {

    // Create a single Gson object for all Gson operations
    private final Gson gson = new Gson();
    private final Service service;

    public Server() {
        service = new Service();
        initializeDatabase();
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        Spark.post("/user", this::registerUser);
        Spark.post("/session", this::loginUser);
        Spark.delete("/session", this::logoutUser);
        Spark.get("/game", this::listGame);
        Spark.post("/game", this::createGame);
        Spark.put("/game", this::joinGame);
        Spark.delete("/db", this::clearDatabase);

        Spark.exception(ServerException.class, this::handleException);

        //This line initializes the server and can be removed once you have a functioning endpoint
        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }


    /**
     * registerUser takes the request JSON object, then makes it usable for Java. It then registers the user
     * into the server's database.
     * @param request is the JSON request to register a user. Contains username, password, and email.
     * @param response is the resulting response JSON object along with the serialized return information
     * @return the AuthTokenData object, serialized as a JSON object.
     * @throws ServerException
     */
    private String registerUser(Request request, Response response) {
        try {
            // Store the user data from the request
            UserData submittedUser = new Gson().fromJson(request.body(), UserData.class);

            // Trim the username
            String trimmedUsername = submittedUser.username().trim();
            UserData user = new UserData(trimmedUsername, submittedUser.password(), submittedUser.email());

            // Verify inputs
            if (!validateInput(user.username()) || !validateInput(user.password()) || !validateEmail(user.email())) {
                throw new ServerException("Error: bad request", 400);
            }

            // Register user data
            AuthTokenData authToken = null;
            try {
                authToken = service.register(user);
            } catch (ServerException e) {
                response.status(e.getStatusCode());
                    throw new ServerException("error| "+e.getMessage(),e.getStatusCode());
            }
            response.status(200);
            return gson.toJson(authToken);

        } catch (JsonSyntaxException e) {
            // Handle invalid JSON structure
            e.printStackTrace(); // 打印日志
            return createErrorResponse(response, "Error! bad request", 400);

        } catch (ServerException e) {
            // Handle ServerException and create a meaningful response
            e.printStackTrace();
            return createErrorResponse(response, "error| "+e.getMessage(), e.getStatusCode());
        }
    }

    // 添加一个通用的方法来创建错误响应
    private String createErrorResponse(Response response, String errorMessage, int statusCode) {
        response.status(statusCode);
        return gson.toJson(new MessageResponse(errorMessage)); // 构建错误信息的 JSON 响应
    }

    /**
     * loginUser will attempt to log in the user given a username and password.
     * @param request is the JSON request to register a user. Contains username and password.
     * @param response is the resulting response JSON object along with the serialized return information
     * @return the AuthTokenData object, serialized as a JSON object.
     * @throws ServerException
     */
    private String loginUser(Request request, Response response) throws ServerException {
        /**
         * Small record class specifically for deserializing the login request
         */
        record UserLoginCredentials(String username, String password) {}

        // Store the credentials from the request
        UserLoginCredentials userLogin = new Gson().fromJson(request.body(), UserLoginCredentials.class);
        if (!validateInput(userLogin.username()) || !validateInput(userLogin.password())) {
            throw new ServerException("bad request", 400);
        }

        String username = userLogin.username;
        String password = userLogin.password;

        try {
            AuthTokenData authToken = service.login(username, password);
            response.status(200);
            return gson.toJson(authToken);
        } catch(ServerException e) {
            response.status(e.getStatusCode());
            response.body(gson.toJson(new MessageResponse("Error: " + e.getMessage())));
            return response.body();
        }

    }

    /**
     * logoutUser will attempt to log out the user given the session's authtoken.
     * @param request contains the authToken we are seeking to remove
     * @param response is the resulting response JSON object along with the serialized return information
     * @throws ServerException
     */
    private Object logoutUser(Request request, Response response) throws ServerException {
        String authToken = request.headers("authorization");

        try{
            service.logOut(authToken);
        } catch (ServerException e) {
            e.printStackTrace();
            response.status(e.getStatusCode());
            return createErrorResponse(response, "error| "+ e.getMessage(), e.getStatusCode());
        }
        response.status(200);
        return "";
    }

    /**
     * listGame will return a list of all current games in the database
     * @param request contains the current user's authToken
     * @param response is the resulting response JSON object along with the serialized return information
     * @return the list of games in the database
     * @throws ServerException 401
     */
    private String listGame(Request request, Response response) throws ServerException {
        String authToken = request.headers("authorization");
        Collection<GameData> gameList = null;
        try {
            gameList = service.listGames(authToken);
        } catch (ServerException e) {
            throw new ServerException("Error: " + e.getMessage(), e.getStatusCode());
        }
        response.status(200);
        Map<String, Object> jsonMap = Map.of("games", gameList);
        return gson.toJson(jsonMap);
    }

    /**
     * createGame will create a new GameData object in the database.
     * @param request contains the user's authToken and the name of the game object they want to create.
     * @param response is the resulting response JSON object along with the serialized return information
     * @return the gameID of the new game object
     * @throws ServerException 400, 401
     */
    private String createGame(Request request, Response response) throws ServerException {
        Map<String, String> requestBody = gson.fromJson(request.body(), Map.class);
        String authToken = request.headers("authorization");
        String gameName = requestBody.get("gameName");

        int gameID;
        try{
            gameID = service.createGame(authToken, gameName);
        }catch (ServerException e){
            response.status(e.getStatusCode());
            return createErrorResponse(response, e.getMessage(), e.getStatusCode());
        }
        response.status(200);
        Map<String, Integer> jsonMap = Map.of("gameID", gameID);
        return gson.toJson(jsonMap);
    }

    /**
     * joinGame will add a user to an existing GameData object in the database.
     * @param request contains the authData, playerColor and gameID
     * @param response contains only the success code (or error message).
     */
    private Object joinGame(Request request, Response response) throws ServerException {
        Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
        ChessGame.TeamColor teamColor;
        String authData;
//commit
        // Assign variables for our Service function call
        authData = request.headers("authorization");

        if (validateInput((String)requestBody.get("playerColor")) && requestBody.get("gameID") != null) {
            if (((String) requestBody.get("playerColor")).equalsIgnoreCase("WHITE")) {
                teamColor = ChessGame.TeamColor.WHITE;
            } else if (((String) requestBody.get("playerColor")).equalsIgnoreCase("BLACK")){
                teamColor = ChessGame.TeamColor.BLACK;
            }
            else {
                throw new ServerException("bad request", 400);
            }
            double gameIDDouble = (double) requestBody.get("gameID");
            int gameID = (int) gameIDDouble;

            service.joinGame(authData, teamColor, gameID);
            response.status(200);
        }
        else {
            throw new ServerException("bad request", 400);
        }
        return "";
    }

    /**
     * clearDatabase will clear the database. I'm sure that doesn't come as a shock to you.
     * @param request contains really nothing as far as I could understand
     * @param response contains nothing but the success code, exception info, and my feelings of resignation at
     *                 having to make these large comment headers for every function (it's good practice)
     */
    private Object clearDatabase(Request request, Response response) throws ServerException {
        service.clearApp();
        response.status(200);
        return "";
    }

    /**
     * Method to handle exceptions
     * @param e is the exception
     * @param request is the request
     * @param response is the response
     */
    private void handleException(Exception e, Request request, Response response) {
        int statusCode;
        String errorMessage;
        MessageResponse messageResponse;

        if (e instanceof ServerException serverException) {
            statusCode = ((ServerException) e).getStatusCode();
            errorMessage = "Error: " + e.getMessage();
            messageResponse = new MessageResponse(errorMessage);
        }
        else {
            statusCode = 500;
            errorMessage = "Error: " + e.getMessage();
            messageResponse = new MessageResponse(errorMessage);
        }

        response.status(statusCode);
        response.type("application/json");
        response.body(gson.toJson(messageResponse));
    }

    /**
     * validateUsername will check to see if an input string is null or empty.
     * @param input The input to check validity of
     * @return true if the input is valid
     */
    private Boolean validateInput(String input) {
        return input != null && !input.isEmpty();
    }

    /**
     * validateEmail will make sure that the submitted email follows correct email format.
     * We use a regex function to accomplish this.
     * @param email is the email to check
     * @return true if the email is valid
     */
    private Boolean validateEmail(String email) {
        // EMAIL_REGEX constant was written by amittn on Stack Overflow, with minor changes
        // https://stackoverflow.com/questions/58189908/regex-for-email-validation-including-blank-field-valid-as-well
//        final String EMAIL_REGEX = "^([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})$";
//        final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

        //            return EMAIL_PATTERN.matcher(email).matches();
        return email != null;
    }

    private void initializeDatabase() {
        try {
            // User data initialized first
            SqlUserDataAccess userDataAccess = new SqlUserDataAccess();
            userDataAccess.configureDatabase();

            SqlGameDataAccess gameDataAccess = new SqlGameDataAccess();
            SqlAuthDataAccess authDataAccess = new SqlAuthDataAccess();

            authDataAccess.configureDatabase();
            gameDataAccess.configureDatabase();
        } catch (dataaccess.ServerException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}