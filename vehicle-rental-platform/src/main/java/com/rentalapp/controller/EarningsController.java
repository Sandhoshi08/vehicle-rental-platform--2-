package com.rentalapp.controller;

import com.rentalapp.dto.EarningsDtos.EarningsSummary;
import com.rentalapp.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/earnings")
@RequiredArgsConstructor
public class EarningsController {

    private final EarningsService earningsService;

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/mine")
    public ResponseEntity<EarningsSummary> myEarnings(Authentication auth) {
        return ResponseEntity.ok(earningsService.getOwnerEarnings(auth.getName()));
    }
}
