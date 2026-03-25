package models;

import com.google.gson.Gson;

public record UserData(String username, String password, String email) {

    // 【修改说明】：删除了未使用的 setUsername() 方法

    public String toString() {
        return new Gson().toJson(this);
    }
}