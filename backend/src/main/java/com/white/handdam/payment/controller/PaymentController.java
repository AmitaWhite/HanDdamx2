package com.white.handdam.payment.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.SliceResponse;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentDetailResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.dto.response.PaymentSummaryResponse;
import com.white.handdam.payment.service.PaymentService;
import com.white.handdam.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/me")
    public ApiResponse<SliceResponse<PaymentSummaryResponse>> getMyPayments(
            @AuthenticationPrincipal AuthMember authMember,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SliceResponse<PaymentSummaryResponse> response =
                SliceResponse.from(paymentService.getMyPayments(authMember.id(), pageable));

        return ApiResponse.success(response);
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDetailResponse> getPayment(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long paymentId
    ) {
        PaymentDetailResponse response = paymentService.getPayment(authMember.id(), paymentId);

        return ApiResponse.success(response);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/prepare")
    public ApiResponse<PaymentPrepareResponse> prepare(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PaymentPrepareRequest request
    ) {
        PaymentPrepareResponse response = paymentService.prepare(authMember.id(), request);

        return ApiResponse.success(response);
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirm(authMember.id(), request);

        return ApiResponse.success(response);
    }

    @PostMapping("/fail")
    public ApiResponse<PaymentFailResponse> fail(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PaymentFailRequest request
    ) {
        PaymentFailResponse response = paymentService.fail(authMember.id(), request);

        return ApiResponse.success(response);
    }
}
