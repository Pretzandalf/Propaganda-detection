package ru.isntrui.recipe_generator.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.isntrui.recipe_generator.entities.User;
import ru.isntrui.recipe_generator.queries.ChangePasswordRequest;
import ru.isntrui.recipe_generator.queries.JwtAuthenticationResponse;
import ru.isntrui.recipe_generator.queries.SignInRequest;
import ru.isntrui.recipe_generator.queries.SignUpRequest;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SEmailService emailService;
    @Value("${spring.application.name}")
    private String applicationName;

    public JwtAuthenticationResponse signUp(@NonNull SignUpRequest request) {

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
        try {
            userService.create(user);
        } catch (Exception exception) {
            throw new RuntimeException("пользователь существует");
        }
        var jwt = jwtService.generateToken(user);
        emailService.sendHtmlEmail(user.getEmail(), "Welcome!", """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Welcome to advanced ckooking!</title>
                  <style>
                    body {
                      font-family: Helvetica, Arial, sans-serif;
                      background-color: #000000;
                      margin: 0;
                      padding: 0;
                      color: #ffffff;
                    }
                    .container {
                      width: 100%;
                      max-width: 600px;
                      margin: 0 auto;
                      padding: 20px;
                      border: 1px solid #444444;
                      border-radius: 8px;
                    }
                    .header {
                      text-align: center;
                      padding: 20px 0;
                      border-bottom: 1px solid #444444;
                    }
                    .header h1 {
                      font-size: 24px;
                      margin: 0;
                    }
                    .content {
                      padding: 20px 0;
                    }
                    .content p {
                      font-size: 16px;
                      line-height: 1.5;
                      margin-bottom: 20px;
                    }
                    .footer {
                      text-align: center;
                      padding-top: 20px;
                      font-size: 12px;
                      color: #bbbbbb;
                    }
                  </style>
                </head>
                <body>
                <div class="container">
                  <div class="header">
                    <h1>Welcome to advanced ckooking!</h1>
                  </div>
                  <div class="content">
                    <p>Thank you for registering with <b>advanced ckooking</b>! We're thrilled to have you on board!</p>
                    <p>Your account is now active, and you can start exploring our services right away.</p>
                    <p>If you have any questions or need assistance, feel free to contact our support team at any time.</p>
                  </div>
                  <div class="footer">
                    <p>&copy; 2025 advanced ckooking. All rights reserved.</p>
                  </div>
                </div>
                </body>
                </html>
                """);

        return new JwtAuthenticationResponse(jwt);
    }

    /**
     * Аутентификация пользователя
     *
     * @param request данные пользователя
     * @return токен
     */
    public JwtAuthenticationResponse signIn(@NonNull SignInRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        ));

        var user = userService
                .userDetailsService()
                .loadUserByUsername(request.getUsername());
        
        var jwt = jwtService.generateToken(user);
        return new JwtAuthenticationResponse(jwt);
    }

    public void changePassword(@NonNull ChangePasswordRequest cp) throws RuntimeException{
        User user;
        try {
            user = userService.getUserByEmail(cp.email());
        } catch (Exception ex) {
            throw new RuntimeException("Пользователь не найден");
        }
        if (!passwordEncoder.matches(cp.oldPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }
        user.setPassword(passwordEncoder.encode(cp.password()));
        userService.changePassword(user.getEmail(), user.getPassword());
    }
}
