package ru.isntrui.recipe_generator.queries;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Text {
    String text;
    String detectedLanguageCode;
}