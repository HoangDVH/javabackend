package com.hoang.jwtjava.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Thuật toán ký theo code mẫu Java chính thức VNPay (pay.html).
 * hashData = fieldName=URLEncoder(value)&... (tên field không encode)
 * query    = URLEncoder(fieldName)=URLEncoder(value)&...
 */
final class VnpaySignatureUtil {

    private VnpaySignatureUtil() {
    }

    static String sign(Map<String, String> params, String hashSecret) {
        return hmacSha512(hashSecret, buildHashData(params));
    }

    /** Chuỗi dùng để ký HMAC — giống iterator trong demo VNPay. */
    static String buildHashData(Map<String, String> params) {
        return buildSignedStrings(params).hashData();
    }

    /** Chuỗi query URL — cả key lẫn value đều encode. */
    static String buildQueryString(Map<String, String> params) {
        return buildSignedStrings(params).query();
    }

    static String buildPaymentUrl(String payUrl, Map<String, String> params, String hashSecret) {
        SignedStrings signed = buildSignedStrings(params);
        String secureHash = hmacSha512(hashSecret, signed.hashData());
        return payUrl + "?" + signed.query() + "&vnp_SecureHash=" + secureHash;
    }

    static boolean verify(Map<String, String> params, String hashSecret, String secureHash) {
        if (secureHash == null || secureHash.isBlank())
            return false;
        String expected = sign(params, hashSecret);
        return constantTimeEquals(expected, secureHash);
    }

    /**
     * Logic giống hệt vòng lặp trong tài liệu VNPay Java:
     * chỉ append &amp; khi field có giá trị và còn phần tử tiếp theo trong iterator.
     */
    private static SignedStrings buildSignedStrings(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(encode(fieldValue));
                query.append(encode(fieldName));
                query.append('=');
                query.append(encode(fieldValue));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        return new SignedStrings(hashData.toString(), query.toString());
    }

    private static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result)
                sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign VNPay request", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length())
            return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++)
            result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }

    private record SignedStrings(String hashData, String query) {
    }
}
