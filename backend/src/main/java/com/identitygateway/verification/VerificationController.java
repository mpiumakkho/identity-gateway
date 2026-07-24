package com.identitygateway.verification;

import com.identitygateway.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    @GetMapping("/methods")
    public ApiResponse<List<VerificationMethodResponse>> methods() {
        return ApiResponse.ok(List.of(
                new VerificationMethodResponse("DIP_CHIP", "Dip Chip", "Read citizen card data from a supported reader.", true),
                new VerificationMethodResponse("MANUAL_ENTRY", "Manual Entry", "Capture citizen data through a controlled form.", true)
        ));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<VerificationSessionResponse>> startSession(
            @Valid @RequestBody StartVerificationRequest request
    ) {
        VerificationSessionResponse session = new VerificationSessionResponse(
                UUID.randomUUID(),
                request.method(),
                "CREATED",
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(session));
    }
}