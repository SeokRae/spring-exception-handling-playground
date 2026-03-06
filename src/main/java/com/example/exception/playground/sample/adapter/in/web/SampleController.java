package com.example.exception.playground.sample.adapter.in.web;

import com.example.exception.playground.global.exception.AccessDeniedException;
import com.example.exception.playground.global.exception.BusinessRuleViolationException;
import com.example.exception.playground.global.exception.DuplicateResourceException;
import com.example.exception.playground.global.exception.NotFoundException;
import com.example.exception.playground.global.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/samples")
public class SampleController {

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        if (id == 0) {
            throw new NotFoundException("Sample with id 0 not found");
        }
        return ResponseEntity.ok(Map.of("id", id, "name", "sample"));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody SampleRequest request) {
        if ("duplicate".equals(request.name())) {
            throw new DuplicateResourceException("Sample with name 'duplicate' already exists");
        }
        return ResponseEntity.ok(Map.of("result", "created", "name", request.name()));
    }

    @GetMapping("/type-mismatch")
    public ResponseEntity<Map<String, Object>> typeMismatch(@RequestParam Integer number) {
        return ResponseEntity.ok(Map.of("number", number));
    }

    @GetMapping("/missing-param")
    public ResponseEntity<Map<String, String>> missingParam(@RequestParam String required) {
        return ResponseEntity.ok(Map.of("required", required));
    }

    @GetMapping("/unauthorized")
    public ResponseEntity<Void> unauthorized() {
        throw new UnauthorizedException("Invalid or expired token");
    }

    @GetMapping("/access-denied")
    public ResponseEntity<Void> accessDenied() {
        throw new AccessDeniedException("Insufficient permissions to access this resource");
    }

    @PostMapping("/business-rule")
    public ResponseEntity<Map<String, String>> businessRule(@Valid @RequestBody SampleRequest request) {
        if (request.age() > 100) {
            throw new BusinessRuleViolationException("Age cannot exceed 100 for this operation");
        }
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    @GetMapping("/unexpected-error")
    public ResponseEntity<Void> unexpectedError() {
        throw new RuntimeException("Something went terribly wrong");
    }
}
