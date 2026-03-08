package com.example.exception.playground.sample.adapter.in.web;

import com.example.exception.playground.global.exception.GatewayErrorException;
import com.example.exception.playground.global.exception.GatewayTimeoutException;
import com.example.exception.playground.global.exception.RequestInProgressException;
import com.example.exception.playground.global.exception.ServiceUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/samples")
public class SampleGatewayController {

    @GetMapping("/gateway-error")
    public ResponseEntity<Void> gatewayError() {
        throw new GatewayErrorException("Payment service connection refused");
    }

    @GetMapping("/gateway-timeout")
    public ResponseEntity<Void> gatewayTimeout() {
        throw new GatewayTimeoutException("Payment service did not respond within 5000ms");
    }

    @GetMapping("/service-unavailable")
    public ResponseEntity<Void> serviceUnavailable() {
        throw new ServiceUnavailableException("Payment service is under maintenance");
    }

    @GetMapping("/request-in-progress")
    public ResponseEntity<Void> requestInProgress() {
        throw new RequestInProgressException("Request is being processed by payment service", 30);
    }

    @GetMapping("/request-in-progress-no-retry")
    public ResponseEntity<Void> requestInProgressNoRetry() {
        throw new RequestInProgressException("Request is being processed by payment service");
    }
}
