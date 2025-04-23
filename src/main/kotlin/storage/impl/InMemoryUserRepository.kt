package com.example.storage.impl

import com.example.model.User
import com.example.storage.UserRepository
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserRepository : UserRepository {

    private val users = ConcurrentHashMap<String, User>()
    private val usersByLogin = ConcurrentHashMap<String, User>()

    override suspend fun save(user: User): User {
        users[user.id] = user
        usersByLogin[user.login] = user
        return user
    }

    override suspend fun findById(id: String): User? {
        return users[id]
    }

    override suspend fun findByLogin(login: String): User? {
        return usersByLogin[login]
    }

    override suspend fun getAllUsers(): List<User> {
        return users.values.toList()
    }
}
