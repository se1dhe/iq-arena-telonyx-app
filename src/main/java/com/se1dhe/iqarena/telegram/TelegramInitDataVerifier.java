package com.se1dhe.iqarena.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

// Проверяет Telegram Web App initData по официальной схеме HMAC-SHA256.
@Component
public class TelegramInitDataVerifier {
    private final String botToken;

    public TelegramInitDataVerifier(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
    }

    public VerifiedTelegramUser verify(String initData) {
        Map<String, String> params = parseQuery(initData);
        String receivedHash = params.remove("hash");

        if (receivedHash == null || receivedHash.isBlank()) {
            throw new IllegalArgumentException("Telegram initData не содержит hash");
        }

        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        String calculatedHash = hmacHex(hmacBytes("WebAppData".getBytes(StandardCharsets.UTF_8), botToken), dataCheckString);
        if (!MessageDigest.isEqual(calculatedHash.getBytes(StandardCharsets.UTF_8), receivedHash.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Некорректная подпись Telegram initData");
        }

        long authDate = Long.parseLong(params.getOrDefault("auth_date", "0"));
        if (authDate <= 0 || Instant.ofEpochSecond(authDate).isBefore(Instant.now().minusSeconds(86400))) {
            throw new IllegalArgumentException("Telegram initData устарел");
        }

        String userJson = params.get("user");
        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("Telegram initData не содержит user");
        }

        return VerifiedTelegramUser.fromJson(userJson);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                result.put(urlDecode(pair[0]), urlDecode(pair[1]));
            }
        }
        return result;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private byte[] hmacBytes(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось рассчитать HMAC", e);
        }
    }

    private String hmacHex(byte[] key, String value) {
        byte[] digest = hmacBytes(key, value);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
