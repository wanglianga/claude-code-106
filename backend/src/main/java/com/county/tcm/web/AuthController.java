package com.county.tcm.web;

import com.county.tcm.domain.UserAccount;
import com.county.tcm.repo.UserRepository;
import com.county.tcm.security.CurrentUserProvider;
import com.county.tcm.security.JwtService;
import com.county.tcm.service.DomainSupport;
import com.county.tcm.web.dto.Dtos;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CurrentUserProvider currentUser;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder, JwtService jwt,
                          CurrentUserProvider currentUser) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Dtos.LoginRequest req) {
        UserAccount user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!encoder.matches(req.password(), user.getPassword()) || !user.isEnabled()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwt.generate(user.getId(), user.getUsername(), user.getRole().name());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("user", DomainSupport.userView(user));
        return m;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return DomainSupport.userView(currentUser.require());
    }
}
