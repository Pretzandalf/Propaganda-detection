package ru.isntrui.recipe_generator.controllers;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.isntrui.recipe_generator.entities.User;
import ru.isntrui.recipe_generator.queries.Translation;
import ru.isntrui.recipe_generator.queries.YandexCloudIAMToken;
import ru.isntrui.recipe_generator.repositories.UserRepository;
import ru.isntrui.recipe_generator.services.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RestController
@RequestMapping("/api/p")
public class PropController {
    private final UserService userService;
    private final UserRepository userRepository;
    @Value("${cloud.yandex.oauth}")
    private String yaOauth;
    @Value("${cloud.yandex.folder}")
    private String folderId;

    private String iamToken = "";

    public PropController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 3600000)
    private void updateToken() {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>("{\"yandexPassportOauthToken\":\"" + yaOauth + "\"}");
        String url = "https://iam.api.cloud.yandex.net/iam/v1/tokens";
        ResponseEntity<YandexCloudIAMToken> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                YandexCloudIAMToken.class
        );
        System.out.println(response.getBody());
        iamToken = Objects.requireNonNull(response.getBody()).getIamToken();
    }

    @PostMapping
    public ResponseEntity<String> get(@RequestBody String text) {
        RestTemplate restTemplate = new RestTemplate();

        // Configure RestTemplate to use UTF-8
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpEntity<?> entity = HttpEntity.EMPTY;
        if (text.isBlank()) return ResponseEntity.badRequest().build();
        if (!isEnglish(text)) {
            String transUrl = "https://translate.api.cloud.yandex.net/translate/v2/translate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + iamToken);
            headers.set("Accept-Encoding", "gzip, deflate, br");
            HttpEntity<?> entity1 = new HttpEntity<>("{     \"folderId\": \"%s\", \"texts\": [\"%s\"],     \"targetLanguageCode\": \"en\" } ".formatted(folderId, text), headers);

            ResponseEntity<Translation> translationResponseEntity = restTemplate.exchange(
                    transUrl,
                    HttpMethod.POST,
                    entity1,
                    Translation.class
            );
            text = translationResponseEntity.getBody().getTranslations().get(0).getText();
        }

        String url = "http://localhost:8000/prop/g?text=" + text;

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );
        HttpHeaders h = new HttpHeaders();
        h.add("Content-Type", "application/json");
        return new ResponseEntity<>("{\"result\": \"" + response.getBody() + "\"}", h, HttpStatus.OK);
    }

    private boolean isEnglish(String text) {
        return text.matches("[a-zA-Z0-9\\s\\p{Punct}]+");
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validate() {
        System.out.println("heheh");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user")
    public ResponseEntity<User> user() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/mfaQR")
    public ResponseEntity<byte[]> qr() {
        Pair<byte[], Pair<String, String>> p = userService.generateQr();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", p.getSecond().getFirst());
        headers.set("Token", p.getSecond().getSecond());
        User u = userService.getCurrentUser();
        if (u.getTotpSecret() != null) return ResponseEntity.badRequest().build();
        u.setTotpSecret(p.getSecond().getSecond());
        userRepository.save(u);
        return new ResponseEntity<>(p.getFirst(), headers, HttpStatus.OK);
    }

    @PostMapping("/mfa")
    public ResponseEntity<Void> mfa(@RequestBody String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        boolean successful = verifier.isValidCode(userService.getCurrentUser().getTotpSecret(), code);
        System.out.println(successful);
        if (successful) return ResponseEntity.ok().build();
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/ismfa")
    public ResponseEntity<String> isMfa() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return new ResponseEntity("{\"e\": " + (!userService.getCurrentUser().getTotpSecret().equals(null) || !userService.getCurrentUser().getTotpSecret().isBlank()) + "}", headers, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Void> upDate(@RequestBody User user) {
        User u = userService.getCurrentUser();
        u.setFirstName(user.getFirstName());
        u.setLastName(user.getLastName());
        userService.updateUser(u);
        System.out.println("hehehehehe");
        System.out.println(u);
        return ResponseEntity.ok().build();
    }
}
