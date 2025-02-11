package ru.isntrui.recipe_generator.queries;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class Translation {
    List<Text> translations;
}

