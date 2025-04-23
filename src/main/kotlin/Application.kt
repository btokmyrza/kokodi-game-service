package com.example

import com.example.auth.AuthService
import com.example.auth.JwtConfig
import com.example.game.GameService
import com.example.plugins.configureRouting
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.storage.GameRepository
import com.example.storage.UserRepository
import com.example.storage.impl.InMemoryGameRepository
import com.example.storage.impl.InMemoryUserRepository
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

val coreModule = module {
    single<UserRepository> { InMemoryUserRepository() }
    single<GameRepository> { InMemoryGameRepository() }
}

fun Application.module() {
    val appConfig = environment.config

    install(Koin) {
        slf4jLogger()
        modules(coreModule, createEnvironmentModule(appConfig))
    }

    configureSecurity()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}

fun createEnvironmentModule(config: ApplicationConfig): Module =
    module {
        single { config }
        single { JwtConfig(config = get()) }
        single {
            AuthService(
                userRepository = get(),
                jwtConfig = get(),
            )
        }
        single {
            GameService(
                gameRepository = get(),
                userRepository = get(),
                config = get(),
            )
        }
    }
