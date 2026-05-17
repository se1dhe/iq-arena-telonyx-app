# IQ Arena — Telegram-first план

## Главное решение

IQ Arena на первом этапе — это не отдельная iOS/macOS игра, а Telegram-native PvP quiz.

Игрок взаимодействует с продуктом через:

- Telegram Bot
- Telegram Web App
- Telegram share/invite mechanics
- Telegram группы/каналы для вирусности

## Компоненты MVP

### 1. Telegram Bot

Задачи бота:

- `/start`
- регистрация игрока
- кнопка `Играть`
- кнопка `Профиль`
- кнопка `Топ игроков`
- кнопка `Пригласить друга`
- отправка результата матча
- deep links для инвайтов

### 2. Telegram Web App

Задачи Web App:

- красивый игровой UI
- авторизация через Telegram initData
- поиск соперника
- экран матча
- экран вопроса
- таймер
- выбор ответа
- reveal правильного ответа
- финальный результат
- рейтинг и прогресс

### 3. Backend

Задачи backend:

- проверка Telegram initData
- создание/обновление Player по telegramUserId
- выдача JWT/session token для Web App
- выдача wsTicket
- matchmaking
- server-authoritative match engine
- scoring
- leaderboard
- anti-cheat метрики

## MVP flow

```text
Telegram /start
  -> bot показывает меню
  -> пользователь нажимает Играть
  -> открывается Telegram Web App
  -> Web App отправляет initData на backend
  -> backend проверяет подпись Telegram
  -> backend создает/находит Player
  -> Web App получает accessToken
  -> Web App получает wsTicket
  -> Web App подключается к /ws
  -> queue.join
  -> match.found
  -> round.open x5
  -> round.reveal x5
  -> match.result
```

## Приоритеты разработки

### Этап 1

- Telegram initData auth endpoint
- TelegramUser поля в Player
- Bot command `/start`
- кнопка Web App
- простой Web App экран-заглушка

### Этап 2

- WebSocket auth через wsTicket
- matchmaking queue
- создание матча на 2 игроков

### Этап 3

- полноценные 5 раундов
- scoring
- result
- leaderboard

### Этап 4

- referrals
- daily challenge
- share result
- категории вопросов

## Что не делаем сейчас

- отдельный iOS клиент
- отдельный macOS клиент
- Steam
- Unity/Godot
- 3D
- App Store релиз

Вся логика должна быть построена так, чтобы позже можно было подключить отдельный клиент, но MVP должен жить в Telegram.
