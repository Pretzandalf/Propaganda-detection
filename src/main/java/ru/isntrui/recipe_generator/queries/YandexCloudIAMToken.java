package ru.isntrui.recipe_generator.queries;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class YandexCloudIAMToken {
    private String iamToken;
    private LocalDateTime expiresAt;
}
