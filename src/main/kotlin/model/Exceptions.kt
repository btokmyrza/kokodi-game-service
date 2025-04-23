package com.example.model

class AuthenticationException(
    message: String = "Authentication failed",
) : RuntimeException(message)

class AuthorizationException(
    message: String = "Authorization required",
) : RuntimeException(message)

class UserAlreadyExistsException(
    login: String,
) : RuntimeException("User with login '$login' already exists.")

class UserNotFoundException(
    identifier: String,
) : RuntimeException("User '$identifier' not found.")

class GameNotFoundException(
    gameId: String,
) : RuntimeException("Game with ID '$gameId' not found.")

class InvalidGameStateException(
    message: String,
) : RuntimeException(message)

class GameFullException(
    gameId: String
) : RuntimeException("Game '$gameId' is already full.")

class NotPlayerTurnException(
    userId: String,
    gameId: String,
) : RuntimeException("It's not user '$userId' turn in game '$gameId'.")

class PlayerNotInGameException(
    userId: String,
    gameId: String,
) : RuntimeException("User '$userId' is not in game '$gameId'.")

class InvalidActionTargetException(
    message: String,
) : RuntimeException(message)

class DeckEmptyException(
    gameId: String,
) : RuntimeException("Deck is empty in game '$gameId'. Game cannot continue.")
