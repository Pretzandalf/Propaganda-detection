package ru.isntrui.recipe_generator.queries;
import lombok.Data;

import java.util.List;

@Data
public class Recipe {
    private String title;
    private List<String> ingredients;
    private List<String> directions;
}