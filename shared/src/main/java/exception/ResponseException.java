package exception;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * This exception class is similar to the ServerException server class, but with additional code to convert to/from Json
 */
public class ResponseException extends Exception {
    final private int statusCode;

    public ResponseException(String message, int code) {
        super(message);
        statusCode = code;
    }

    public String toJson() {
        return new Gson().toJson(Map.of("message", getMessage(), "status", statusCode));
    }

    public static ResponseException fromJson(InputStream inputStream) {
        try {
            var map = new Gson().fromJson(new InputStreamReader(inputStream), HashMap.class);

            int code = 500;
            if (map != null && map.get("status") != null) {
                code = ((Double) map.get("status")).intValue();
            }

            String message = "Unknown error";
            if (map != null && map.get("message") != null) {
                message = map.get("message").toString();
            }

            return new ResponseException(message, code);
        } catch (Exception e) {
            return new ResponseException("Error response parsing error: " + e.getMessage(), 500);
        }
    }


    public int getStatusCode() {
        return statusCode;
    }
}
