import com.example.auth.AuthService
import com.example.auth.JwtConfig
import com.example.model.AuthenticationException
import com.example.model.User
import com.example.model.UserAlreadyExistsException
import com.example.model.dto.LoginRequest
import com.example.model.dto.RegisterRequest
import com.example.storage.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthServiceTest {

    private lateinit var authService: AuthService

    private val userRepository: UserRepository = mockk()
    private val jwtConfig: JwtConfig = mockk()

    @BeforeEach
    fun setup() {
        authService = AuthService(
            userRepository = userRepository,
            jwtConfig = jwtConfig,
        )
    }

    @Test
    fun `registerUser saves new user when not exists`(): TestResult = runTest {
        val request = RegisterRequest(
            login = "newuser",
            password = "password",
            name = "Test User",
        )
        val expectedUser = User(
            id = "1",
            login = "newuser",
            password = "password",
            name = "newuser",
        )

        coEvery { userRepository.findByLogin("newuser") } returns null
        coEvery { userRepository.save(any()) } returns expectedUser

        val result = authService.registerUser(request)

        assertEquals("newuser", result.login)
        assertEquals("password", result.password)
        assertEquals("newuser", result.name)
        coVerify {
            userRepository.save(
                user = match { user ->
                    user.login == request.login
                },
            )
        }
    }

    @Test
    fun `registerUser throws UserAlreadyExistsException when user exists`(): TestResult = runTest {
        val request = RegisterRequest(
            login = "existing",
            password = "pass",
            name = "Existing User",
        )
        val existingUser = User(
            id = "1",
            login = "pass",
            password = "Existing User",
            name = "existing",
        )

        coEvery { userRepository.findByLogin("existing") } returns existingUser

        assertThrows<UserAlreadyExistsException> {
            authService.registerUser(request)
        }

        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `loginUser returns LoginResponse when credentials are valid`() = runTest {
        val id = "123"
        val login = "validUserLogin"
        val name = "validUserName"

        val user = User(
            id = id,
            login = login,
            password = "correct",
            name = name,
        )
        val request = LoginRequest(
            login = login,
            password = "correct",
        )
        val expectedToken = "test.token.123"

        coEvery { userRepository.findByLogin(login) } returns user
        coEvery { jwtConfig.createToken(id) } returns expectedToken

        val result = authService.loginUser(request)

        assertEquals(expectedToken, result.token)
        assertEquals(id, result.userId)
        assertEquals(name, result.name)
        coVerify { jwtConfig.createToken(id) }
    }

    @Test
    fun `loginUser throws AuthenticationException when user not found`(): TestResult = runTest {
        val request = LoginRequest(
            login = "unknown",
            password = "password",
        )

        coEvery { userRepository.findByLogin("unknown") } returns null

        assertThrows<AuthenticationException> {
            authService.loginUser(request)
        }
    }

    @Test
    fun `loginUser throws AuthenticationException when password is incorrect`() = runTest {
        val user = User(
            id = "123",
            login = "validuser",
            password = "password",
            name = "validuser",
        )
        val request = LoginRequest(
            login = "validuser",
            password = "wrong",
        )

        coEvery { userRepository.findByLogin("validuser") } returns user

        assertThrows<AuthenticationException> {
            authService.loginUser(request)
        }
    }

    @Test
    fun `loginResponse contains correct user details`() = runTest {
        val user = User(
            id = "456",
            login = "testuser",
            password = "pass",
            name = "testuser",
        )
        val request = LoginRequest(
            login = "testuser",
            password = "pass",
        )
        val expectedToken = "generated.token.456"

        coEvery { userRepository.findByLogin("testuser") } returns user
        coEvery { jwtConfig.createToken("456") } returns expectedToken

        val response = authService.loginUser(request)

        assertEquals(expectedToken, response.token)
        assertEquals("456", response.userId)
        assertEquals("testuser", response.name)
    }
}
