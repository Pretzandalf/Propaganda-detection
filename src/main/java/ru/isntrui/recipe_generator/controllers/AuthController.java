package ru.isntrui.recipe_generator.controllers;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.isntrui.recipe_generator.entities.User;
import ru.isntrui.recipe_generator.queries.ChangePasswordRequest;
import ru.isntrui.recipe_generator.queries.JwtAuthenticationResponse;
import ru.isntrui.recipe_generator.queries.SignInRequest;
import ru.isntrui.recipe_generator.queries.SignUpRequest;
import ru.isntrui.recipe_generator.services.AuthenticationService;
import ru.isntrui.recipe_generator.services.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;
    private final UserService userService;
    @PostMapping("sign-up")
    public ResponseEntity<JwtAuthenticationResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        try {
            JwtAuthenticationResponse r = authenticationService.signUp(request);
            return ResponseEntity.ok().body(r);
        } catch (Exception ex) {
            System.out.println(ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("sign-in")
    public ResponseEntity<JwtAuthenticationResponse> signIn(@RequestBody @Valid SignInRequest request) {
        try {
            return ResponseEntity.ok().body(authenticationService.signIn(request));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }



    @PutMapping("changePassword")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest cp) {
        try {
            authenticationService.changePassword(cp);
            return ResponseEntity.ok("Пароль успешно изменён");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}

