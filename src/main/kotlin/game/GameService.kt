package com.example.game

import com.example.model.Card
import com.example.model.CardType
import com.example.model.DeckEmptyException
import com.example.model.GameFullException
import com.example.model.GameNotFoundException
import com.example.model.GameSession
import com.example.model.GameState
import com.example.model.InvalidActionTargetException
import com.example.model.InvalidGameStateException
import com.example.model.NotPlayerTurnException
import com.example.model.PlayerNotInGameException
import com.example.model.TurnLogEntry
import com.example.model.UserNotFoundException
import com.example.model.dto.GameStatusResponse
import com.example.model.dto.PlayerScoreInfo
import com.example.model.dto.TurnRequest
import com.example.model.dto.TurnResponse
import com.example.storage.GameRepository
import com.example.storage.UserRepository
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class GameService(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    config: ApplicationConfig,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val winScore = config.property("game.win_score").getString().toInt()
    private val gameLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun createGame(creatorUserId: String): GameSession {
        val creatorUser = userRepository.findById(creatorUserId)
            ?: throw UserNotFoundException(creatorUserId)

        val newGame = GameSession()
        newGame.playerIds.add(creatorUser.id)
        newGame.scores[creatorUser.id] = 0

        logger.info("Game ${newGame.id} created by User ${creatorUser.id} (${creatorUser.name})")
        return gameRepository.save(newGame)
    }

    suspend fun joinGame(gameId: String, userId: String): GameSession {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        val mutex = getGameLock(gameId)

        mutex.withLock {
            val game = gameRepository.findById(gameId) ?: throw GameNotFoundException(gameId)

            if (game.state != GameState.WAITING_FOR_PLAYERS) {
                throw InvalidGameStateException(
                    "Game $gameId is not waiting for players. Current state: ${game.state}",
                )
            }
            if (game.playerIds.size >= game.maxPlayers) {
                throw GameFullException(gameId)
            }
            if (game.playerIds.contains(userId)) {
                throw InvalidGameStateException("User $userId (${user.name}) is already in game $gameId")
            }

            game.playerIds.add(userId)
            game.scores[userId] = 0
            logger.info("User ${user.id} (${user.name}) joined game $gameId. Players: ${game.playerIds.size}")

            return gameRepository.save(game)
        }
    }

    suspend fun startGame(gameId: String, userId: String): GameSession {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        val mutex = getGameLock(gameId)

        mutex.withLock {
            val game = gameRepository.findById(gameId) ?: throw GameNotFoundException(gameId)
            val playerIds = game.playerIds
            val state = game.state
            val playerIdsCount = playerIds.size
            val minPlayers = game.minPlayers

            if (!playerIds.contains(userId)) {
                throw PlayerNotInGameException(userId, gameId)
            }
            if (state != GameState.WAITING_FOR_PLAYERS) {
                throw InvalidGameStateException(
                    "Game $gameId cannot be started. Current state: $state",
                )
            }
            if (playerIdsCount < minPlayers) {
                throw InvalidGameStateException(
                    "Game $gameId needs at least $minPlayers players to start. Has: $playerIdsCount",
                )
            }

            game.state = GameState.IN_PROGRESS
            game.deck = DeckFactory.createShuffledDeck()
            game.turnOrder = game.playerIds.shuffled()
            game.currentPlayerIndex = 0
            game.isNextPlayerSkipped = false
            game.winnerId = null
            game.gameEndMessage = null

            game.turnOrder.forEach { pId -> game.scores.putIfAbsent(pId, 0) }

            logger.info(
                "Game $gameId started by User ${user.id} (${user.name}). " +
                        "Turn order: ${game.turnOrder}. " +
                        "Deck size: ${game.deck.size}",
            )

            return gameRepository.save(game)
        }
    }

    suspend fun playTurn(gameId: String, userId: String, request: TurnRequest): TurnResponse {
        val mutex = getGameLock(gameId)

        mutex.withLock {
            val game = gameRepository.findById(gameId) ?: throw GameNotFoundException(gameId)
            val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)

            val state = game.state
            val turnOrder = game.turnOrder
            val currentPlayerIndex = game.currentPlayerIndex

            if (state != GameState.IN_PROGRESS) {
                throw InvalidGameStateException(
                    "Game $gameId is not in progress. Current state: $state",
                )
            }
            if (turnOrder.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= turnOrder.size) {
                throw InvalidGameStateException(
                    "Game $gameId has invalid turn order state.",
                )
            }

            val currentPlayerId = turnOrder[currentPlayerIndex]
            if (userId != currentPlayerId) {
                throw NotPlayerTurnException(userId, gameId)
            }

            if (game.isNextPlayerSkipped) {
                game.isNextPlayerSkipped = false
                val skippedPlayerId = turnOrder[currentPlayerIndex]
                val skippedPlayerName = userRepository.findById(skippedPlayerId)?.name
                val description = "Player $skippedPlayerId ($skippedPlayerName) turn was skipped due to Block card."
                logger.info("Game $gameId: $description")

                advanceTurn(game)
                gameRepository.save(game)

                val currentScores = getCurrentPlayerScoreInfo(game)
                return TurnResponse(
                    cardPlayed = Card(
                        name = "Skipped",
                        type = CardType.ACTION,
                        value = 0,
                    ),
                    description = description,
                    updatedScores = currentScores,
                    currentPlayerId = turnOrder[game.currentPlayerIndex].takeIf {
                        game.state == GameState.IN_PROGRESS
                    },
                    nextPlayerId = getNextPlayerId(game),
                    isGameOver = game.state == GameState.FINISHED,
                    winnerId = game.winnerId,
                    gameEndMessage = game.gameEndMessage,
                )
            }

            if (game.deck.isEmpty()) {
                game.state = GameState.FINISHED
                game.gameEndMessage = "Deck is empty. Game finished as a draw."

                logger.warn("Game $gameId: Deck is empty. Finishing game.")

                gameRepository.save(game)

                throw DeckEmptyException(gameId)
            }

            val card = game.deck.removeFirst()
            var description = "Player $userId (${user.name}) played ${card.name}. "

            when (card.type) {
                CardType.POINTS -> {
                    val currentScore = game.scores.getOrDefault(userId, 0)
                    val newScore = min(currentScore + card.value, winScore)

                    game.scores[userId] = newScore
                    description += "Score increased by ${card.value} to $newScore."
                }
                CardType.ACTION -> {
                    when (card.name) {
                        Card.BLOCK -> {
                            game.isNextPlayerSkipped = true
                            description += "The next player will miss their turn."
                        }
                        Card.DOUBLE_DOWN -> {
                            val currentScore = game.scores.getOrDefault(userId, 0)
                            val newScore = min(currentScore * card.value, winScore)

                            game.scores[userId] = newScore
                            description += "Score doubled to $newScore."
                        }
                        else -> {
                            if (card.name.startsWith(Card.STEAL)) {
                                val targetPlayerId = request.targetPlayerId
                                    ?: throw InvalidActionTargetException(
                                        "Target player ID is required for Steal action.",
                                    )
                                if (targetPlayerId == userId) {
                                    throw InvalidActionTargetException(
                                        "Cannot target yourself for Steal action.",
                                    )
                                }

                                val targetPlayer = userRepository.findById(targetPlayerId)
                                    ?: throw InvalidActionTargetException(
                                        "Target player $targetPlayerId not found.",
                                    )
                                if (!game.playerIds.contains(targetPlayerId)) {
                                    throw InvalidActionTargetException(
                                        "Target player $targetPlayerId (${targetPlayer.name}) is not in this game.",
                                    )
                                }

                                val targetScore = game.scores.getOrDefault(targetPlayerId, 0)
                                val amountToSteal = min(card.value, targetScore)

                                if (amountToSteal > 0) {
                                    game.scores[targetPlayerId] = targetScore - amountToSteal
                                    val currentScore = game.scores.getOrDefault(userId, 0)

                                    game.scores[userId] = min(currentScore + amountToSteal, winScore)

                                    description += "Stole $amountToSteal points from ${targetPlayer.name}. "
                                    description += "Player score is now ${game.scores[userId]}, "
                                    description += "Target score is now ${game.scores[targetPlayerId]}."
                                } else {
                                    description += "Attempted to steal from ${targetPlayer.name}, but they had no points."
                                }
                            } else {
                                logger.warn("Game $gameId: Unhandled action card type: ${card.name}")
                                description += "Unknown action effect."
                            }
                        }
                    }
                }
            }

            logger.info("Game $gameId: $description")

            val turnNumber = game.turnHistory.size + 1
            game.turnHistory.add(
                TurnLogEntry(
                    turnNumber = turnNumber,
                    playerId = userId,
                    cardPlayed = card,
                    description = description,
                    scoresSnapshot = game.scores.toMap(),
                )
            )

            if (game.scores.getOrDefault(userId, 0) >= winScore) {
                game.state = GameState.FINISHED
                game.winnerId = userId
                game.gameEndMessage = "Player $userId (${user.name}) reached $winScore points and won!"

                logger.info("Game $gameId: ${game.gameEndMessage}")
            }

            if (game.state == GameState.IN_PROGRESS) {
                advanceTurn(game)
            }

            gameRepository.save(game)

            val finalScores = getCurrentPlayerScoreInfo(game)
            return TurnResponse(
                cardPlayed = card,
                description = description,
                updatedScores = finalScores,
                currentPlayerId = game.turnOrder[game.currentPlayerIndex].takeIf {
                    game.state == GameState.IN_PROGRESS
                },
                nextPlayerId = getNextPlayerId(game).takeIf { game.state == GameState.IN_PROGRESS },
                isGameOver = game.state == GameState.FINISHED,
                winnerId = game.winnerId,
                gameEndMessage = game.gameEndMessage,
            )
        }
    }

    private fun advanceTurn(game: GameSession) {
        if (game.turnOrder.isEmpty()) {
            return
        }

        game.currentPlayerIndex = (game.currentPlayerIndex + 1) % game.turnOrder.size

        logger.debug(
            "Game ${game.id}: Advanced turn. " +
                    "Next player index: ${game.currentPlayerIndex} (ID: ${game.turnOrder[game.currentPlayerIndex]})",
        )
    }

    suspend fun getGameStatus(gameId: String): GameStatusResponse {
        val game = gameRepository.findById(gameId) ?: throw GameNotFoundException(gameId)

        val playerInfos = game.playerIds.mapNotNull { pId ->
            userRepository.findById(pId)?.let { user ->
                PlayerScoreInfo(
                    userId = pId,
                    name = user.name,
                    score = game.scores.getOrDefault(key = pId, defaultValue = 0),
                )
            }
        }

        val currentPlayerId = game.turnOrder.getOrNull(game.currentPlayerIndex).takeIf {
            game.state == GameState.IN_PROGRESS && game.currentPlayerIndex >= 0
        }

        return GameStatusResponse(
            gameId = game.id,
            state = game.state,
            players = playerInfos,
            cardsRemaining = game.deck.size,
            currentPlayerId = currentPlayerId,
            nextPlayerId = getNextPlayerId(game).takeIf { game.state == GameState.IN_PROGRESS },
            isNextPlayerSkipped = game.isNextPlayerSkipped,
            winnerId = game.winnerId,
            gameEndMessage = game.gameEndMessage,
        )
    }

    private fun getGameLock(gameId: String): Mutex {
        return gameLocks.computeIfAbsent(gameId) { Mutex() }
    }

    private suspend fun getCurrentPlayerScoreInfo(game: GameSession): List<PlayerScoreInfo> {
        return game.playerIds.mapNotNull { pId ->
            userRepository.findById(pId)?.let { user ->
                PlayerScoreInfo(
                    userId = pId,
                    name = user.name,
                    score = game.scores.getOrDefault(pId, 0),
                )
            }
        }
    }

    private fun getNextPlayerId(game: GameSession): String? {
        if (game.state != GameState.IN_PROGRESS || game.turnOrder.isEmpty()) {
            return null
        }

        val nextIndex = (game.currentPlayerIndex + 1) % game.turnOrder.size

        return if (game.isNextPlayerSkipped) {
            val skippedPlayerIndex = nextIndex
            val actualNextIndex = (skippedPlayerIndex + 1) % game.turnOrder.size

            game.turnOrder.getOrNull(actualNextIndex)
        } else {
            game.turnOrder.getOrNull(nextIndex)
        }
    }
}
