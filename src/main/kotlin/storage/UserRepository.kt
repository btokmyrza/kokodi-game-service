package com.example.storage

import com.example.model.User

interface UserRepository {

    suspend fun save(user: User): User

    suspend fun findById(id: String): User?

    suspend fun findByLogin(login: String): User?

    suspend fun getAllUsers(): List<User>
}
