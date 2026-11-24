package com.example.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Profile(
        @NotBlank @Size(max = 30) String phoneNumber,
        @NotBlank @Size(max = 200) String address
) {
}
