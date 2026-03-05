package com.example.exception.playground.sample;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SampleRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(min = 2, max = 20, message = "Name must be between 2 and 20 characters")
        String name,

        @Min(value = 1, message = "Age must be at least 1")
        int age
) {
}
