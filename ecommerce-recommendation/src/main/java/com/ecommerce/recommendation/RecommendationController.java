package com.ecommerce.recommendation;

import com.ecommerce.common.result.Result;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
class RecommendationController {
    private final RecommendationService service;

    RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/me")
    ResponseEntity<Result<RecommendationService.Feed>> me(@RequestHeader("X-User-Id") long userId,
                                                            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.ok(service.feed(userId, limit(limit))));
    }

    @GetMapping("/popular")
    Result<RecommendationService.Feed> popular(@RequestParam(defaultValue = "12") int limit) {
        return Result.ok(service.feed(null, limit(limit)));
    }

    @PostMapping("/events")
    Result<Void> events(@RequestHeader("X-User-Id") long userId,
                        @RequestBody List<RecommendationService.BrowserEvent> events) {
        if (events.isEmpty() || events.size() > 30 || events.stream().anyMatch(event ->
                event == null || event.type() == null || event.spuId() == null || event.spuId() <= 0
                        || event.type() == RecommendationEventType.CART_ADD || event.type() == RecommendationEventType.PAID)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recommendation events");
        }
        service.recordBrowserEvents(userId, events);
        return Result.ok();
    }

    private int limit(int limit) {
        if (limit < 1 || limit > 24) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..24");
        return limit;
    }
}
