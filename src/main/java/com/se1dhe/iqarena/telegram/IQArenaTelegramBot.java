package com.se1dhe.iqarena.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Заготовка Telegram Bot слоя. Полную регистрацию long polling добавим после проверки версии TelegramBots API.
@Slf4j
@Component
public class IQArenaTelegramBot implements ApplicationRunner {
    private final String username;
    private final String webAppUrl;

    public IQArenaTelegramBot(
            @Value("${telegram.bot.username}") String username,
            @Value("${telegram.webapp.url}") String webAppUrl
    ) {
        this.username = username;
        this.webAppUrl = webAppUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("IQ Arena Telegram bot bootstrap loaded: username={}, webAppUrl={}", username, webAppUrl);
    }
}
