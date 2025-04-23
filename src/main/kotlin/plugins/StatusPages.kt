package com.example.plugins

import com.example.model.AuthenticationException
import com.example.model.AuthorizationException
import com.example.model.DeckEmptyException
import com.example.model.GameFullException
import com.example.model.GameNotFoundException
import com.example.model.InvalidActionTargetException
import com.example.model.InvalidGameStateException
import com.example.model.NotPlayerTurnException
import com.example.model.PlayerNotInGameException
import com.example.model.UserAlreadyExistsException
import com.example.model.UserNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(val error: String)

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception: ${cause.localizedMessage}", cause)

            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse("Internal server error: ${cause.localizedMessage}"),
            )
        }
        exception<AuthenticationException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = ErrorResponse(cause.message ?: "Authentication failed"),
            )
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = ErrorResponse(cause.message ?: "Forbidden"),
            )
        }
        exception<UserAlreadyExistsException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Conflict,
                message = ErrorResponse(cause.message ?: "User already exists"),
            )
        }
        exception<UserNotFoundException> { call, cause ->
            call.respond(
                status = HttpStatusCode.NotFound,
                message = ErrorResponse(cause.message ?: "User not found"),
            )
        }
        exception<GameNotFoundException> { call, cause ->
            call.respond(
                status = HttpStatusCode.NotFound,
                message = ErrorResponse(cause.message ?: "Game not found"),
            )
        }
        exception<GameFullException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Conflict,
                message = ErrorResponse(cause.message ?: "Game is full"),
            )
        }
        exception<InvalidGameStateException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorResponse(cause.message ?: "Invalid game state"),
            )
        }
        exception<NotPlayerTurnException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = ErrorResponse(cause.message ?: "Not your turn"),
            )
        }
        exception<PlayerNotInGameException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = ErrorResponse(cause.message ?: "Player not in game"),
            )
        }
        exception<InvalidActionTargetException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorResponse(cause.message ?: "Invalid action target"),
            )
        }
        exception<DeckEmptyException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Conflict,
                message = ErrorResponse(cause.message ?: "Deck is empty, game over"),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorResponse(cause.message ?: "Bad request"),
            )
        }
    }
}