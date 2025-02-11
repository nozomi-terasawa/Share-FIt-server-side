package org.example.project

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.example.project.infrastructure.auth.AuthJwt
import org.example.project.infrastructure.database.initDatabase
import org.example.project.infrastructure.repositoryImpl.*
import org.example.project.infrastructure.routes.fitnessRoutes
import org.example.project.infrastructure.routes.geoFenceRoutes
import org.example.project.infrastructure.routes.passedRoutes
import org.example.project.infrastructure.routes.userRoutes
import org.example.project.usecases.fitness.GetFitnessUseCase
import org.example.project.usecases.fitness.SaveFitnessUseCase
import org.example.project.usecases.location.EntryGeofenceUseCase
import org.example.project.usecases.location.ExitFeoFenceUseCase
import org.example.project.usecases.location.FetchGeoFenceUseCase
import org.example.project.usecases.passed.TodayPassedUserGetUseCase
import org.example.project.usecases.user.UserCreateUseCase
import org.example.project.usecases.user.UserDeleteUseCase
import org.example.project.usecases.user.UserLogOutUseCase
import org.example.project.usecases.user.UserLoginUseCase

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()

    val authJwt = AuthJwt(secret, issuer, audience, myRealm)

    install(ContentNegotiation) {
        json()
    }

    install(WebSockets) {
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Authentication) {
        // JWTの設定
        jwt("auth-jwt") {
            realm = myRealm
            verifier(authJwt.verifier())
            validate {
                val name = it.payload.getClaim("userEmail").asString()
                if (name != null) {
                    JWTPrincipal(it.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(status = HttpStatusCode.Unauthorized, message = "Unauthorized")
            }
        }
    }

    initDatabase()

    // 実実装のDI
    val userRepositoryImpl = UserRepositoryImpl()
    val userInfoRepositoryImpl = UserInfoRepositoryImpl()
    val geoFenceRepository = GeoFenceRepositoryImpl()
    val fitnessRepository = FitnessRepositoryImpl()

    // ユーザーのUseCaseをDI
    val userCreateUseCase = UserCreateUseCase(userRepositoryImpl, authJwt)
    val userLoginUseCase = UserLoginUseCase(userRepositoryImpl, authJwt)
    val userLogoutUseCase = UserLogOutUseCase(userRepositoryImpl)
    val userDeleteUseCase = UserDeleteUseCase(userRepositoryImpl)

    // ジオフェンス関係のUseCaseをDI
    val entryGeofenceUseCase = EntryGeofenceUseCase(geoFenceRepository)
    val exitFeoFenceUseCase = ExitFeoFenceUseCase(geoFenceRepository)
    val fetchGeoFenceUseCase = FetchGeoFenceUseCase(geoFenceRepository)

    // フィットネス関係のUseCaseをDI
    val saveFitnessUseCase = SaveFitnessUseCase(fitnessRepository)
    val getFitnessUseCase = GetFitnessUseCase(fitnessRepository)

    // すれ違い
    val passedUserRepository = PassedUserRepositoryImpl()
    val todayPassedUserGetUseCase = TodayPassedUserGetUseCase(passedUserRepository, userInfoRepositoryImpl)

    routing {
        userRoutes(
            userCreateUseCase,
            userLoginUseCase,
            userLogoutUseCase,
            userDeleteUseCase,
        )
        geoFenceRoutes(
            entryGeofenceUseCase = entryGeofenceUseCase,
            exitFeoFenceUseCase = exitFeoFenceUseCase,
            fetchGeoFenceUseCase = fetchGeoFenceUseCase
        )
        fitnessRoutes(
            saveFitnessUseCase = saveFitnessUseCase,
            getFitnessUseCase = getFitnessUseCase,
        )
        passedRoutes(
            todayPassedUserGetUseCase = todayPassedUserGetUseCase,
        )
    }
}
