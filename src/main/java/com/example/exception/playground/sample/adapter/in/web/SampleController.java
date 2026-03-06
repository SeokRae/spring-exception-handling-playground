package com.example.exception.playground.sample.adapter.in.web;

import com.example.exception.playground.global.exception.DuplicateResourceException;
import com.example.exception.playground.global.exception.NotFoundException;
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
}
