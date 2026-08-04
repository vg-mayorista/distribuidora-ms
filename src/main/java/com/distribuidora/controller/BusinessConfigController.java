package com.distribuidora.controller;

import com.distribuidora.dto.config.BusinessConfigResponse;
import com.distribuidora.dto.config.UpdateBusinessConfigRequest;
import com.distribuidora.service.BusinessConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class BusinessConfigController {

    private final BusinessConfigService businessConfigService;

    @GetMapping("/public")
    public ResponseEntity<BusinessConfigResponse> getPublicConfig() {
        return ResponseEntity.ok(businessConfigService.getPublicConfig());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR')")
    public ResponseEntity<BusinessConfigResponse> updateConfig(@Valid @RequestBody UpdateBusinessConfigRequest request) {
        return ResponseEntity.ok(businessConfigService.updateConfig(request));
    }
}
