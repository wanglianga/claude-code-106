package com.tcm.controller;

import com.tcm.config.AuthUtils;
import com.tcm.config.JwtService;
import com.tcm.model.User;
import com.tcm.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq req) {
        User user = users.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", jwt.generate(user.getUsername(), user.getRole().name()));
        body.put("user", user);
        return body;
    }

    @GetMapping("/me")
    public User me() {
        return AuthUtils.current();
    }
}
