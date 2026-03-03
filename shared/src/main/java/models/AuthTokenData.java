package models;

import com.google.gson.Gson;

public record AuthTokenData(String authToken, String username) {

    public String toString() {
        return new Gson().toJson(this);
    }
}