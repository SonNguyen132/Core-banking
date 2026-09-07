package com.finaegis.presentation.rest;

import com.finaegis.presentation.dto.AuthenticationRequest;
import com.finaegis.presentation.dto.AuthenticationResponse;
import com.finaegis.presentation.dto.RegisterRequest;
import com.finaegis.security.AuthenticationService;
import com.finaegis.security.JwtService;
import com.finaegis.security.User;
import com.finaegis.security.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<User> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found")));
    }
}
