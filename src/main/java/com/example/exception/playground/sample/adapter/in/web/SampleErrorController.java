package com.example.exception.playground.sample.adapter.in.web;

import com.example.exception.playground.global.exception.AccessDeniedException;
import com.example.exception.playground.global.exception.BusinessRuleViolationException;
import com.example.exception.playground.global.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/samples")
public class SampleErrorController {

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
