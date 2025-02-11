package ru.isntrui.recipe_generator.services;

import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isntrui.recipe_generator.entities.User;
import ru.isntrui.recipe_generator.repositories.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User create(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        return save(user);
    }

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }


    @Transactional
    public void changePassword(String email, String newPassword) {
        User user = getUserByEmail(email);
        userRepository.updatePassword(user.getId(), newPassword);
    }

    @Transactional
    public void remove(String email) {
        User user = getUserByEmail(email);
        userRepository.delete(user);
    }

    @Transactional
    public void remove(UUID id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) throws RuntimeException {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException(email + " not found"));
    }

    public User getUserById(UUID id) throws RuntimeException {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException(id + " not found"));
    }

    public UserDetailsService userDetailsService() {
        return this::getByUsername;
    }

    public User getCurrentUser() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getByUsername(username);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

    }

    public void setAvatar(UUID id, String url) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException(id + " doesnt exist"));
        user.setAvatarUrl(url);
        userRepository.save(user);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    @SneakyThrows
    public Pair<byte[], Pair<String, String>> generateQr() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator(50);
        String secret = secretGenerator.generate();
        QrData data = new QrData.Builder()
                .label(getCurrentUser().getUsername())
                .secret(secret)
                .issuer("Propaganda Check")
                .algorithm(HashingAlgorithm.SHA512)
                .digits(6)
                .period(30)
                .build();
        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = generator.generate(data);
        String mimeType = generator.getImageMimeType();
        return Pair.of(imageData, Pair.of(mimeType, secret));
    }
}