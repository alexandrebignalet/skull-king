package org.skull.king

import CorsFilter
import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.skull.king.application.component.DaggerSkullKingComponent
import org.skull.king.application.component.SkullKingComponent
import org.skull.king.application.config.SkullKingConfig
import org.skull.king.application.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.application.infrastructure.authentication.User
import org.skull.king.application.module.ConfigurationModule
import org.skull.king.application.utils.JsonObjectMapper
import org.skull.king.application.web.controller.healthcheck.BaseHealthCheck
import org.skull.king.application.web.exception.DomainErrorExceptionMapper
import org.skull.king.game_room.infrastructure.web.GameRoomExceptionMapper
import org.slf4j.LoggerFactory


class SkullkingApplication : Application<SkullKingConfig>() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SkullkingApplication::class.java)

        @JvmStatic
        fun main(vararg args: String) {
            SkullkingApplication().run(*args)
        }
    }

    override fun initialize(bootstrap: Bootstrap<SkullKingConfig>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider,
            EnvironmentVariableSubstitutor(true)
        )
    }

    override fun run(configuration: SkullKingConfig, environment: Environment) {
        environment.run {
            val mapper = JsonObjectMapper.getObjectMapper(environment.objectMapper)
            val skullKingComponent: SkullKingComponent = DaggerSkullKingComponent.builder()
                .configurationModule(ConfigurationModule(configuration, mapper))
                .build()

            // cors
            environment.jersey().register(CorsFilter())
            // auth
            environment.jersey().register(
                AuthDynamicFeature(
                    OAuthCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(skullKingComponent.provideFirebaseAuthenticator())
                        .setPrefix(FirebaseAuthenticator.PREFIX)
                        .buildAuthFilter()
                )
            )
            // use @Auth to inject User in resource
            environment.jersey().register(AuthValueFactoryProvider.Binder(User::class.java))

            // healthchecks
            healthChecks().register("base", BaseHealthCheck())

            // exception mapper
            jersey().register(DomainErrorExceptionMapper())
            jersey().register(GameRoomExceptionMapper())

            // controllers
            jersey().register(skullKingComponent.provideSkullKingResource())
            jersey().register(skullKingComponent.provideGameRoomResource())
        }
    }
}
