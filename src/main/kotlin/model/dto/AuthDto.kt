package com.example.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String,
    val name: String,
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
)
