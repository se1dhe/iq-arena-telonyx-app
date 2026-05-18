package com.se1dhe.iqarena.telegram;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
public class IQArenaTelegramBot implements ApplicationRunner {
    private static final String START_TEXT = """
            IQ Arena готова.

            Жми кнопку ниже, заходи в арену и собирай рейтинг в быстрых PvP-викторинах.
            """;

    private final String token;
    private final String username;
    private final String webAppUrl;
    private TelegramBotsLongPollingApplication botsApplication;
    private TelegramClient telegramClient;
    private BotSession botSession;

    public IQArenaTelegramBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String username,
            @Value("${telegram.webapp.url}") String webAppUrl
    ) {
        this.token = token;
        this.username = username;
        this.webAppUrl = webAppUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (token == null || token.isBlank() || "replace-me".equals(token)) {
            log.warn("Telegram bot token is not configured; long polling is disabled");
            return;
        }

        try {
            telegramClient = new OkHttpTelegramClient(token);
            botsApplication = new TelegramBotsLongPollingApplication();
            botSession = botsApplication.registerBot(token, this::consumeUpdates);
            log.info(
                    "IQ Arena Telegram bot registered: username={}, webAppUrl={}, running={}",
                    username,
                    webAppUrl,
                    botSession.isRunning()
            );
        } catch (TelegramApiException e) {
            log.error("Failed to register IQ Arena Telegram bot", e);
        }
    }

    private void consumeUpdates(List<Update> updates) {
        updates.forEach(this::handleUpdate);
    }

    private void handleUpdate(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }

        String text = message.getText();
        if (text == null || text.isBlank() || text.startsWith("/start")) {
            sendStartMessage(message.getChatId());
            return;
        }

        sendStartMessage(message.getChatId());
    }

    private void sendStartMessage(Long chatId) {
        if (chatId == null || telegramClient == null) {
            return;
        }

        SendMessage response = new SendMessage(chatId.toString(), START_TEXT);
        response.setReplyMarkup(playKeyboard());

        try {
            telegramClient.execute(response);
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram start message to chatId={}", chatId, e);
        }
    }

    private InlineKeyboardMarkup playKeyboard() {
        InlineKeyboardButton playButton = new InlineKeyboardButton("Играть");
        playButton.setWebApp(new WebAppInfo(webAppUrl));
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(playButton)));
    }

    @PreDestroy
    public void shutdown() {
        if (botSession != null) {
            botSession.stop();
        }
        if (botsApplication != null) {
            try {
                botsApplication.close();
            } catch (Exception e) {
                log.warn("Failed to close Telegram long polling application", e);
            }
        }
    }
}
