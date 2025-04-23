package com.example.plugins

import com.example.auth.AuthService
import com.example.game.GameService
import com.example.model.AuthenticationException
import com.example.model.dto.CreateGameResponse
import com.example.model.dto.LoginRequest
import com.example.model.dto.RegisterRequest
import com.example.model.dto.TurnRequest
import com.example.storage.GameRepository
import com.example.storage.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService: AuthService by inject()
    val gameService: GameService by inject()
    val userRepo: UserRepository by inject()
    val gameRepo: GameRepository by inject()

    routing {
        get("/debug/users") {
            call.respond(message = userRepo.getAllUsers())
        }
        get("/debug/games") {
            call.respond(message = gameRepo.getAllGames())
        }

        route("/auth") {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                val user = authService.registerUser(request)

                call.respond(
                    status = HttpStatusCode.Created,
                    message = mapOf(
                        "message" to "User ${user.name} registered successfully",
                        "userId" to user.id,
                    ),
                )
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val response = authService.loginUser(request)

                call.respond(response)
            }
        }

        authenticate("jwt") {
            route("/games") {
                post {
                    val principal = call.getUserPrincipal() ?: throw AuthenticationException()
                    val game = gameService.createGame(creatorUserId = principal.userId)

                    call.respond(
                        status = HttpStatusCode.Created,
                        message = CreateGameResponse(
                            gameId = game.id,
                            message = "Game created successfully. Waiting for players.",
                        ),
                    )
                }

                get("/{gameId}") {
                    val gameId = call.parameters["gameId"]
                        ?: throw IllegalArgumentException("Missing gameId parameter")
                    val status = gameService.getGameStatus(gameId)

                    call.respond(status)
                }

                post("/{gameId}/join") {
                    val gameId = call.parameters["gameId"]
                        ?: throw IllegalArgumentException("Missing gameId parameter")
                    val principal = call.getUserPrincipal() ?: throw AuthenticationException()

                    gameService.joinGame(gameId = gameId, userId = principal.userId)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = mapOf("message" to "Successfully joined game $gameId"),
                    )
                }

                post("/{gameId}/start") {
                    val gameId = call.parameters["gameId"]
                        ?: throw IllegalArgumentException("Missing gameId parameter")
                    val principal = call.getUserPrincipal() ?: throw AuthenticationException()

                    gameService.startGame(gameId = gameId, userId = principal.userId)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = mapOf("message" to "Game $gameId started"),
                    )
                }

                post("/{gameId}/turn") {
                    val gameId = call.parameters["gameId"]
                        ?: throw IllegalArgumentException("Missing gameId parameter")
                    val principal = call.getUserPrincipal() ?: throw AuthenticationException()
                    val request = call.receive<TurnRequest>()

                    val turnResult = gameService.playTurn(
                        gameId = gameId,
                        userId = principal.userId,
                        request = request,
                    )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = turnResult,
                    )
                }
            }
        }
    }
}