package com.county.tcm.security;

/** 从 JWT 解析后放入 SecurityContext 的当前登录主体 */
public class LoginUser {
    private final Long id;
    private final String username;
    private final String role;

    public LoginUser(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return username + "(" + role + ")";
    }
}
