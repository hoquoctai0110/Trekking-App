package com.example.trekkingapp.payment;

import com.example.trekkingapp.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{orderCode}")
    public ApiResponse<PaymentStatusResponse> getPayment(@PathVariable Long orderCode) {
        return new ApiResponse<>(true, "Payment retrieved successfully", paymentService.getPaymentStatus(orderCode));
    }

    @PostMapping("/{orderCode}/sync")
    @PreAuthorize("hasAnyRole('TREKKER', 'ADMIN')")
    public ApiResponse<PaymentSyncResponse> syncPaymentStatus(@PathVariable Long orderCode) {
        return new ApiResponse<>(true, "Payment synced successfully", paymentService.syncPaymentStatus(orderCode));
    }

    @PostMapping("/payos/webhook")
    public ApiResponse<String> handleWebhook(@RequestBody PayOSWebhookRequest request) {
        paymentService.handlePayOSWebhook(request);
        return new ApiResponse<>(true, "Webhook processed successfully", "OK");
    }

    @GetMapping("/return")
    public ApiResponse<PaymentReturnResponse> paymentReturn(@RequestParam Map<String, String> params) {
        return new ApiResponse<>(true, "Payment return received", paymentService.handlePaymentReturn(params));
    }

    @GetMapping("/cancel")
    public ApiResponse<PaymentReturnResponse> paymentCancel(@RequestParam Map<String, String> params) {
        return new ApiResponse<>(true, "Payment cancellation received", paymentService.handlePaymentCancel(params));
    }
}
