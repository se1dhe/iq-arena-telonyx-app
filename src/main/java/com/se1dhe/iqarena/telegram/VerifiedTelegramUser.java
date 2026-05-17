package com.se1dhe.iqarena.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Данные Telegram-пользователя после проверки initData.
public record VerifiedTelegramUser(
        Long id,
        String username,
        String firstName,
        String lastName,
        String photoUrl,
        String languageCode
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static VerifiedTelegramUser fromJson(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return new VerifiedTelegramUser(
                    node.path("id").asLong(),
                    textOrNull(node, "username"),
                    textOrNull(node, "first_name"),
                    textOrNull(node, "last_name"),
                    textOrNull(node, "photo_url"),
                    textOrNull(node, "language_code")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный Telegram user JSON", e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
