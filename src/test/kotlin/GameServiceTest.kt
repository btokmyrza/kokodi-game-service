import com.example.game.GameService
import com.example.model.Card
import com.example.model.CardType
import com.example.model.DeckEmptyException
import com.example.model.GameFullException
import com.example.model.GameSession
import com.example.model.GameState
import com.example.model.InvalidGameStateException
import com.example.model.User
import com.example.model.UserNotFoundException
import com.example.model.dto.TurnRequest
import com.example.storage.GameRepository
import com.example.storage.UserRepository
import io.ktor.server.config.ApplicationConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.LinkedList

class GameServiceTest {

    private val gameRepository: GameRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val config: ApplicationConfig = mockk {
        every {
            property("game.win_score")
        } returns mockk {
            every { getString() } returns "100"
        }
    }
    private lateinit var gameService: GameService

    private val testUserId1 = "user1"
    private val testUser1 = User(
        id = testUserId1,
        login = "testUser",
        password = "Test User",
        name = "testUser",
    )
    private val testUserId2 = "user2"
    private val testUser2 = User(
        id = testUserId2,
        login = "testUser2",
        password = "Test User 2",
        name = "testUser2",
    )
    private val testGameId1 = "game1"

    @BeforeEach
    fun setup() {
        gameService = GameService(
            gameRepository = gameRepository,
            userRepository = userRepository,
            config = config,
        )

        coEvery { userRepository.findById(testUserId1) } returns testUser1
        coEvery { userRepository.findById(testUserId2) } returns testUser2
    }

    @Nested
    inner class CreateGameTests {

        @Test
        fun `should create game with creator as first player`() = runTest {
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val game = gameService.createGame(testUserId1)

            assertEquals(1, game.playerIds.size)
            assertTrue(game.playerIds.contains(testUserId1))
            assertEquals(0, game.scores[testUserId1])
            assertEquals(GameState.WAITING_FOR_PLAYERS, game.state)
        }

        @Test
        fun `should throw UserNotFoundException when creator not found`() = runTest {
            coEvery { userRepository.findById("invalid") } returns null

            assertThrows<UserNotFoundException> {
                gameService.createGame("invalid")
            }
        }
    }

    @Nested
    inner class JoinGameTests {

        @Test
        fun `should add player to waiting game`() = runTest {
            val existingGame = GameSession(
                id = testGameId1,
                state = GameState.WAITING_FOR_PLAYERS,
                playerIds = mutableListOf(testUserId1),
                scores = mutableMapOf(testUserId1 to 0),
            )
            val newUser = User(
                id = "user2",
                login = "user2",
                password = "User Two",
                name = "user2",
            )

            coEvery { gameRepository.findById(testGameId1) } returns existingGame
            coEvery { userRepository.findById("user2") } returns newUser
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val updatedGame = gameService.joinGame(testGameId1, "user2")

            assertEquals(2, updatedGame.playerIds.size)
            assertTrue(updatedGame.playerIds.contains("user2"))
            assertEquals(0, updatedGame.scores["user2"])
        }

        @Test
        fun `should throw GameFullException when game is full`() = runTest {
            val fullGame = GameSession(
                id = testGameId1,
                playerIds = MutableList(4) { "user$it" },
                maxPlayers = 4,
            )

            coEvery { gameRepository.findById(testGameId1) } returns fullGame

            assertThrows<GameFullException> {
                gameService.joinGame(
                    gameId = testGameId1,
                    userId = testUserId1,
                )
            }
        }
    }

    @Nested
    inner class StartGameTests {

        @Test
        fun `should start game with sufficient players`() = runTest {
            val game = GameSession(
                id = testGameId1,
                playerIds = mutableListOf("user1", "user2", "user3"),
                state = GameState.WAITING_FOR_PLAYERS
            )

            coEvery { gameRepository.findById(testGameId1) } returns game
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val startedGame = gameService.startGame(testGameId1, testUserId1)

            assertEquals(GameState.IN_PROGRESS, startedGame.state)
            assertTrue(startedGame.deck.isNotEmpty())
            assertEquals(3, startedGame.turnOrder.size)
        }

        @Test
        fun `should throw InvalidGameStateException when not enough players`() = runTest {
            val game = GameSession(
                id = testGameId1,
                playerIds = mutableListOf(testUserId1),
                minPlayers = 2
            )

            coEvery { gameRepository.findById(testGameId1) } returns game

            assertThrows<InvalidGameStateException> {
                gameService.startGame(testGameId1, testUserId1)
            }
        }
    }

    @Nested
    inner class PlayTurnTests {

        @Test
        fun `should process points card correctly`() = runTest {
            val gameSession = getGameSessionWithCard(
                card = Card(
                    name = "Points",
                    type = CardType.POINTS,
                    value = 10,
                ),
            )
            coEvery { gameRepository.findById(testGameId1) } returns gameSession
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val response = gameService.playTurn(
                gameId = testGameId1,
                userId = testUserId1,
                request = TurnRequest(),
            )

            assertEquals(60, response.updatedScores.find { it.userId == testUserId1 }?.score)
            assertEquals("Points", response.cardPlayed.name)
        }

        @Test
        fun `should process block card correctly`() = runTest {
            val gameSession = getGameSessionWithCard(
                card = Card(
                    name = Card.BLOCK,
                    type = CardType.ACTION,
                    value = 0,
                ),
            )
            coEvery { gameRepository.findById(testGameId1) } returns gameSession
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val response = gameService.playTurn(
                gameId = testGameId1,
                userId = testUserId1,
                request = TurnRequest(),
            )

            assertTrue(gameSession.isNextPlayerSkipped)
            assertEquals("user2", response.nextPlayerId)
        }

        @Test
        fun `should process steal card correctly`() = runTest {
            val stealCard = Card(Card.STEAL, CardType.ACTION, 20)
            val game = getGameSessionWithCard(stealCard)
            coEvery { gameRepository.findById(testGameId1) } returns game
            coEvery { userRepository.findById("user2") } returns testUser2
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val response = gameService.playTurn(
                gameId = testGameId1,
                userId = testUserId1,
                request = TurnRequest(targetPlayerId = testUserId2),
            )

            assertEquals(70, response.updatedScores.find { it.userId == testUserId1 }?.score)
            assertEquals(10, response.updatedScores.find { it.userId == testUserId2 }?.score)
        }

        @Test
        fun `should throw DeckEmptyException when no cards left`() = runTest {
            val gameSession = getGameSessionWithCard(
                card = Card(
                    name = "Points",
                    type = CardType.POINTS,
                    value = 10,
                ),
            ).apply {
                deck.clear()
            }
            coEvery { gameRepository.findById(testGameId1) } returns gameSession
            coEvery { gameRepository.save(any()) } returnsArgument 0

            assertThrows<DeckEmptyException> {
                gameService.playTurn(
                    gameId = testGameId1,
                    userId = testUserId1,
                    request = TurnRequest(),
                )
            }
        }

        @Test
        fun `should end game when player reaches win score`() = runTest {
            val game = getGameSessionWithCard(Card("Win", CardType.POINTS, 60)).apply {
                scores[testUserId1] = 90
            }
            coEvery { gameRepository.findById(testGameId1) } returns game
            coEvery { gameRepository.save(any()) } returnsArgument 0

            val response = gameService.playTurn(testGameId1, testUserId1, TurnRequest())

            assertTrue(response.isGameOver)
            assertEquals(testUserId1, response.winnerId)
        }

        private fun getGameSessionWithCard(card: Card): GameSession {
            return GameSession(
                id = testGameId1,
                state = GameState.IN_PROGRESS,
                playerIds = mutableListOf(testUserId1, "user2"),
                turnOrder = listOf(testUserId1, "user2"),
                currentPlayerIndex = 0,
                deck = LinkedList(listOf(card)),
                scores = mutableMapOf(testUserId1 to 50, "user2" to 30),
            )
        }
    }

    @Nested
    inner class GameStatusTests {

        @Test
        fun `should return correct game status`() = runTest {
            val game = GameSession(
                id = testGameId1,
                state = GameState.IN_PROGRESS,
                playerIds = mutableListOf(testUserId1, "user2"),
                turnOrder = listOf(testUserId1, "user2"),
                currentPlayerIndex = 0,
                deck = LinkedList(listOf(Card("Test", CardType.POINTS, 10))),
                scores = mutableMapOf(testUserId1 to 50, "user2" to 30)
            )

            coEvery { gameRepository.findById(testGameId1) } returns game
            coEvery { userRepository.findById("user2") } returns User(
                id = "user2",
                login = "user2",
                password = "User Two",
                name = "user2",
            )

            val status = gameService.getGameStatus(testGameId1)

            assertEquals(2, status.players.size)
            assertEquals(testUserId1, status.currentPlayerId)
            assertEquals("user2", status.nextPlayerId)
            assertEquals(1, status.cardsRemaining)
        }
    }
}
