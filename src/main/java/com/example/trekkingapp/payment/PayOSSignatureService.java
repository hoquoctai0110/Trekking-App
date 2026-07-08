package com.example.trekkingapp.payment;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PayOSSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String sign(Map<String, Object> fields, String checksumKey) {
        String payload = toPayload(fields);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign PayOS payload", exception);
        }
    }

    public boolean verify(Map<String, Object> fields, String checksumKey, String signature) {
        return sign(fields, checksumKey).equalsIgnoreCase(signature);
    }

    public String buildCreatePaymentPayload(PayOSCreatePaymentRequest request) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("amount", request.amount());
        fields.put("cancelUrl", request.cancelUrl());
        fields.put("description", request.description());
        fields.put("orderCode", request.orderCode());
        fields.put("returnUrl", request.returnUrl());
        return toPayload(fields);
    }

    public String signCreatePaymentRequest(PayOSCreatePaymentRequest request, String checksumKey) {
        String payload = buildCreatePaymentPayload(request);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign PayOS payload", exception);
        }
    }

    public Map<String, Object> buildWebhookFields(PayOSWebhookRequest.PayOSWebhookData data) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("amount", data.amount());
        fields.put("code", data.code());
        fields.put("counterAccountBankId", data.counterAccountBankId());
        fields.put("counterAccountBankName", data.counterAccountBankName());
        fields.put("counterAccountName", data.counterAccountName());
        fields.put("counterAccountNumber", data.counterAccountNumber());
        fields.put("currency", data.currency());
        fields.put("desc", data.desc());
        fields.put("description", data.description());
        fields.put("orderCode", data.orderCode());
        fields.put("paymentLinkId", data.paymentLinkId());
        fields.put("reference", data.reference());
        fields.put("transactionDateTime", data.transactionDateTime());
        fields.put("virtualAccountName", data.virtualAccountName());
        fields.put("virtualAccountNumber", data.virtualAccountNumber());
        return fields;
    }

    private String toPayload(Map<String, Object> fields) {
        return fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + renderValue(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String renderValue(Object value) {
        if (value instanceof List<?> listValue) {
            List<String> renderedItems = new ArrayList<>();
            for (Object item : listValue) {
                if (item instanceof Map<?, ?> mapValue) {
                    renderedItems.add(renderMap(castMap(mapValue)));
                } else {
                    renderedItems.add(String.valueOf(item));
                }
            }
            return "[" + String.join(",", renderedItems) + "]";
        }

        if (value instanceof Map<?, ?> mapValue) {
            return renderMap(castMap(mapValue));
        }

        return String.valueOf(value);
    }

    private String renderMap(Map<String, Object> fields) {
        return "{" + fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> entry.getKey() + "=" + renderValue(entry.getValue()))
                .collect(Collectors.joining(",")) + "}";
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        return source.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : Objects.requireNonNull(bytes)) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
