package ru.isntrui.recipe_generator.queries;

import jakarta.annotation.Nullable;

public record ChangePasswordRequest(String email, String password, @Nullable String oldPassword) {
}
