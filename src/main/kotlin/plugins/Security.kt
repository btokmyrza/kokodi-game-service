package com.example.plugins

import com.example.auth.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject

data class UserPrincipal(
    val userId: String,
)

fun Application.configureSecurity() {
    val jwtConfig: JwtConfig by inject()

    install(Authentication) {
        jwt("jwt") {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier)

            validate { credential: JWTCredential ->
                val userId = credential.payload.getClaim("userId").asString()

                if (userId != null) {
                    UserPrincipal(userId)
                } else {
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = "Token is not valid or has expired",
                )
            }
        }
    }
}

fun ApplicationCall.getUserPrincipal(): UserPrincipal? {
    return principal<UserPrincipal>()
}
