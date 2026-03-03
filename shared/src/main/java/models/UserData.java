package models;

import com.google.gson.Gson;

public record UserData(String username, String password, String email) {
    public UserData setUsername(String username) {
        return new UserData(username, this.password, this.email);
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}