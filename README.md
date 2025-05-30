# Микросервис карточной игры (Прототип на Ktor)

Это прототип упрощённого микросервиса для пошаговой карточной игры, написанный на Kotlin с использованием Ktor.

## Возможности

- Регистрация пользователей и аутентификация с помощью JWT
- Создание игры
- Присоединение к играм (от 2 до 4 игроков)
- Запуск игры
- Пошаговый игровой процесс с вытягиванием карт
- Карты с очками (добавляют очки игроку)
- Карты действий:
  - `Block`: следующий игрок пропускает ход
  - `Steal(N)`: украсть N очков у другого игрока (не менее 0)
  - `DoubleDown`: удвоить свои очки (максимум 30)
- Победа: первый игрок, набравший 30 очков
- Получение статуса игры
- Хранение данных в памяти (для прототипа)

**⚠️ Внимание по безопасности:** Необходимо реализовать безопасное хэширование паролей (например, с помощью BCrypt).

## Запуск приложения

### Требования

- Java Development Kit (JDK) 17 или выше
- Gradle (можно использовать встроенный wrapper)
- (Опционально) Docker

### Через Gradle

1. **Сборка:**
    ```bash
    ./gradlew build
    ```
2. **Запуск:**
    ```bash
    ./gradlew run
    ```
   Сервер будет доступен по адресу `http://localhost:8080`.

### Через Docker

1. **Сборка Docker-образа:**
    ```bash
    docker build -t kokodi-game .
    ```
2. **Запуск контейнера:**
    ```bash
    docker run -p 8080:8080 --rm --name kokodi-app kokodi-game
    ```
   Сервер будет доступен по адресу `http://localhost:8080`.

## API Эндпоинты

**Базовый URL:** `http://localhost:8080`

### Аутентификация

- **`POST /auth/register`**
  - Регистрация нового пользователя.
  - **Тело запроса (JSON):**
    ```json
    {
      "login": "newuser",
      "password": "password123",
      "name": "New User Name"
    }
    ```
  - **Ответ:** `201 Created` с сообщением и userId, либо ошибка (`409 Conflict`, если логин уже существует).

- **`POST /auth/login`**
  - Вход пользователя, возвращает JWT токен.
  - **Тело запроса (JSON):**
    ```json
    {
      "login": "newuser",
      "password": "password123"
    }
    ```
  - **Ответ:** `200 OK` с токеном, userId и именем, либо ошибка (`401 Unauthorized`).
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "userId": "user-uuid-string",
      "name": "New User Name"
    }
    ```

### Управление игрой (Требуется авторизация — заголовок `Authorization: Bearer <token>`)

- **`POST /games`**
  - Создание новой игры. Создатель автоматически становится первым игроком.
  - **Ответ:** `201 Created` с ID игры.
    ```json
    {
      "gameId": "game-uuid-string",
      "message": "Игра успешно создана. Ожидание игроков."
    }
    ```

- **`POST /games/{gameId}/join`**
  - Присоединение к существующей игре (если она не заполнена и ожидает игроков).
  - **Параметр пути:** `gameId` — ID игры.
  - **Ответ:** `200 OK`, либо ошибка (`404 Not Found`, `409 Conflict`, `400 Bad Request`).

- **`POST /games/{gameId}/start`**
  - Запуск игры, если в ней достаточно игроков (минимум 2). Только участник игры может инициировать запуск.
  - **Параметр пути:** `gameId` — ID игры.
  - **Ответ:** `200 OK`, либо ошибка (`400 Bad Request`).

- **`GET /games/{gameId}`**
  - Получение текущего состояния игры. Аутентификация может быть обязательной в зависимости от реализации.
  - **Параметр пути:** `gameId`
  - **Ответ:** `200 OK` с деталями состояния игры.
    ```json
    {
      "gameId": "game-uuid-string",
      "state": "IN_PROGRESS",
      "players": [
        { "userId": "user1-uuid", "name": "Игрок 1", "score": 10 },
        { "userId": "user2-uuid", "name": "Игрок 2", "score": 5 }
      ],
      "cardsRemaining": 8,
      "currentPlayerId": "user2-uuid",
      "nextPlayerId": "user1-uuid",
      "isNextPlayerSkipped": false,
      "winnerId": null,
      "gameEndMessage": null
    }
    ```

### Игровой процесс (Требуется авторизация)

- **`POST /games/{gameId}/turn`**
  - Совершение хода текущим игроком: вытягивание и применение карты.
  - **Параметр пути:** `gameId`
  - **Тело запроса (JSON):**
  Для карты Steal:
    ```json
    {
      "targetPlayerId": "другой-игрок-uuid"
    }
    ```
    Для других карт:
    ```json
    {}
    ```
  - **Ответ:** `200 OK` с описанием результата хода, либо ошибка (`403 Forbidden`, `400 Bad Request`, `404 Not Found`, `409 Conflict`).
    ```json
    {
      "cardPlayed": { "name": "Points(5)", "type": "POINTS", "value": 5 },
      "description": "Игрок user1-uuid (Игрок 1) сыграл Points(5). Очки увеличены на 5 до 15.",
      "updatedScores": [
        { "userId": "user1-uuid", "name": "Игрок 1", "score": 15 },
        { "userId": "user2-uuid", "name": "Игрок 2", "score": 5 }
      ],
      "currentPlayerId": "user2-uuid",
      "nextPlayerId": "user1-uuid",
      "isGameOver": false,
      "winnerId": null,
      "gameEndMessage": null
    }
    ```

## TODO / Улучшения

- **Хэширование паролей:** Реализовать безопасное хэширование (например, BCrypt)
- **Безопасность JWT:** Загрузка секретов из переменных окружения
- **Интеграция базы данных:** Заменить хранение в памяти на SQLite или PostgreSQL
- **Обработка ошибок:** Более точные коды и сообщения об ошибках
- **Тестирование:** Добавить больше unit и integration тестов
- **Валидация:** Более строгая проверка входящих данных