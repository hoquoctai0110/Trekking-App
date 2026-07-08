package com.example.trekkingapp.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PayOSClient {

    private static final Logger log = LoggerFactory.getLogger(PayOSClient.class);

    private final RestClient restClient;
    private final PayOSProperties payOSProperties;
    private final PayOSSignatureService payOSSignatureService;

    public PayOSClient(
            RestClient.Builder restClientBuilder,
            PayOSProperties payOSProperties,
            PayOSSignatureService payOSSignatureService
    ) {
        this.restClient = restClientBuilder.build();
        this.payOSProperties = payOSProperties;
        this.payOSSignatureService = payOSSignatureService;
    }

    public PayOSCreatePaymentResponse createPaymentLink(PayOSCreatePaymentRequest request) {
        PayOSCreatePaymentRequest signedRequest = withSignature(request);
        log.info(
                "payos_create_payment_request orderCode={} amount={} description={} itemCount={} returnUrl={} cancelUrl={}",
                signedRequest.orderCode(),
                signedRequest.amount(),
                signedRequest.description(),
                signedRequest.items() == null ? 0 : signedRequest.items().size(),
                signedRequest.returnUrl(),
                signedRequest.cancelUrl()
        );

        try {
            PayOSCreatePaymentResponse response = restClient.post()
                    .uri(payOSProperties.getBaseUrl() + "/v2/payment-requests")
                    .header("x-client-id", payOSProperties.getClientId())
                    .header("x-api-key", payOSProperties.getApiKey())
                    .body(signedRequest)
                    .retrieve()
                    .body(PayOSCreatePaymentResponse.class);

            if (response == null) {
                throw new IllegalStateException("PayOS returned an empty response");
            }

            log.info(
                    "payos_create_payment_response orderCode={} code={} desc={} paymentLinkId={}",
                    signedRequest.orderCode(),
                    response.code(),
                    response.desc(),
                    response.data() == null ? null : response.data().paymentLinkId()
            );

            if (!"00".equals(response.code())) {
                throw new IllegalStateException(response.desc());
            }
            return response;
        } catch (RestClientException exception) {
            log.error("payos_create_payment_failed orderCode={}", signedRequest.orderCode(), exception);
            throw new IllegalStateException("Failed to create PayOS payment link");
        }
    }

    public PayOSGetPaymentLinkResponse getPaymentLinkInformation(Long orderCode) {
        try {
            PayOSGetPaymentLinkResponse response = restClient.get()
                    .uri(payOSProperties.getBaseUrl() + "/v2/payment-requests/" + orderCode)
                    .header("x-client-id", payOSProperties.getClientId())
                    .header("x-api-key", payOSProperties.getApiKey())
                    .retrieve()
                    .body(PayOSGetPaymentLinkResponse.class);

            if (response == null) {
                throw new IllegalStateException("PayOS returned an empty response");
            }

            log.info(
                    "payos_get_payment_response orderCode={} code={} desc={} remoteStatus={}",
                    orderCode,
                    response.code(),
                    response.desc(),
                    response.data() == null ? null : response.data().status()
            );

            if (!"00".equals(response.code())) {
                throw new IllegalStateException(response.desc());
            }
            return response;
        } catch (RestClientException exception) {
            log.error("payos_get_payment_failed orderCode={}", orderCode, exception);
            throw new IllegalStateException("Failed to fetch PayOS payment status");
        }
    }

    private PayOSCreatePaymentRequest withSignature(PayOSCreatePaymentRequest request) {
        String signature = payOSSignatureService.signCreatePaymentRequest(
                request,
                payOSProperties.getChecksumKey()
        );

        return new PayOSCreatePaymentRequest(
                request.orderCode(),
                request.amount(),
                request.description(),
                request.buyerName(),
                request.buyerEmail(),
                request.buyerPhone(),
                request.returnUrl(),
                request.cancelUrl(),
                request.items(),
                signature
        );
    }
}
