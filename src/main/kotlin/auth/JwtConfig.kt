package com.example.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.util.Date

class JwtConfig(config: ApplicationConfig) {

    private val secret = "kokodi-secret"
    private val issuer = "com.example"
    private val audience = "kokodi-users"
    private val validityInMillis = "3600000".toLong()

    val realm = "Kokodi Game Access"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier =
        JWT
            .require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()

    fun createToken(userId: String): String =
        JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withExpiresAt(getExpiration())
            .sign(algorithm)

    private fun getExpiration(): Date =
        Date(System.currentTimeMillis() + validityInMillis)
}
