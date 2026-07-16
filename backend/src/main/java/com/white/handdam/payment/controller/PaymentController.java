package com.white.handdam.payment.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepare(
            // TODO: X-User-Id is temporary and is not real authentication.
            // Remove it after the auth module is complete, then read memberId from SecurityContext or authenticated Principal.
            @RequestHeader("X-User-Id") Long memberId,
            @Valid @RequestBody PaymentPrepareRequest request
    ) {
        PaymentPrepareResponse response = paymentService.prepare(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirm(
            // TODO: X-User-Id is temporary and is not real authentication.
            // Remove it after the auth module is complete, then read memberId from SecurityContext or authenticated Principal.
            @RequestHeader("X-User-Id") Long memberId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirm(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<PaymentFailResponse>> fail(
            // TODO: X-User-Id is temporary and is not real authentication.
            // Remove it after the auth module is complete, then read memberId from SecurityContext or authenticated Principal.
            @RequestHeader("X-User-Id") Long memberId,
            @Valid @RequestBody PaymentFailRequest request
    ) {
        PaymentFailResponse response = paymentService.fail(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
