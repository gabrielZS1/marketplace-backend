package com.marketplace.backend.controller;

import com.marketplace.backend.dto.BusinessStatsResponseDTO;
import com.marketplace.backend.service.BusinessAccessService;
import com.marketplace.backend.service.BusinessStatsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/stats")
public class BusinessStatsController {

    private final BusinessStatsService statsService;
    private final BusinessAccessService accessService;

    public BusinessStatsController(BusinessStatsService statsService,
                                   BusinessAccessService accessService) {
        this.statsService = statsService;
        this.accessService = accessService;
    }

    @GetMapping
    public BusinessStatsResponseDTO stats(
            @PathVariable UUID businessId,
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        accessService.requireOwner(businessId, userId);

        int clamped = Math.max(1, Math.min(days, 365));
        return statsService.build(businessId, clamped);
    }
}
