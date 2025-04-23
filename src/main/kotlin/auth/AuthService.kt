package com.example.auth

import com.example.model.AuthenticationException
import com.example.model.User
import com.example.model.UserAlreadyExistsException
import com.example.model.dto.LoginRequest
import com.example.model.dto.LoginResponse
import com.example.model.dto.RegisterRequest
import com.example.storage.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig,
) {

    suspend fun registerUser(request: RegisterRequest): User {
        if (userRepository.findByLogin(request.login) != null) {
            throw UserAlreadyExistsException(request.login)
        }

        val newUser = User(
            login = request.login,
            password = request.password,
            name = request.name
        )
        return userRepository.save(newUser)
    }

    suspend fun loginUser(request: LoginRequest): LoginResponse {
        val user = userRepository.findByLogin(request.login)
            ?: throw AuthenticationException("Invalid login or password")

        if (request.password != user.password) {
            throw AuthenticationException("Invalid login or password")
        }

        val token = jwtConfig.createToken(user.id)

        return LoginResponse(
            token = token,
            userId = user.id,
            name = user.name,
        )
    }
}
