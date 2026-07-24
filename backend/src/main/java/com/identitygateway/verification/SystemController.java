package com.identitygateway.verification;

import com.identitygateway.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        return ApiResponse.ok(new SystemHealthResponse("identity-gateway", "UP"));
    }
}