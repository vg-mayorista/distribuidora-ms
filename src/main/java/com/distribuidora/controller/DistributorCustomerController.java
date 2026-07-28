package com.distribuidora.controller;

import com.distribuidora.dto.user.CustomerSummaryResponse;
import com.distribuidora.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/distributor/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_DISTRIBUTOR')")
@Tag(name = "Clientes (Distribuidor)", description = "Listado de clientes para contacto y armado")
public class DistributorCustomerController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Listar clientes (solo lectura)")
    public Page<CustomerSummaryResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.getCustomers(search, pageable);
    }
}
