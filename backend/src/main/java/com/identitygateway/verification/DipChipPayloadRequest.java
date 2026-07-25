package com.identitygateway.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DipChipPayloadRequest(
        @NotBlank
        @Pattern(regexp = "\\d{13}", message = "must contain 13 digits")
        String nationalId,

        @NotBlank
        @Size(max = 30)
        String title,

        @NotBlank
        @Size(max = 80)
        String firstName,

        @NotBlank
        @Size(max = 80)
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        @NotBlank
        @Size(min = 8, max = 20)
        String laserCode,

        @NotNull
        @PastOrPresent
        LocalDate cardIssueDate,

        @NotNull
        LocalDate cardExpiryDate,

        @NotBlank
        @Size(max = 80)
        String readerName,

        @NotBlank
        @Size(max = 80)
        String readerSerialNumber,

        @NotBlank
        @Size(max = 10000)
        String rawPayload
) {
}