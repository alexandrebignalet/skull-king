package org.skull.king

import io.dropwizard.Application
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.skull.king.component.DaggerSkullKingComponent
import org.skull.king.component.SkullKingComponent
import org.skull.king.config.SkullKingConfig
import org.skull.king.module.ConfigurationModule
import org.skull.king.utils.JsonObjectMapper
import org.skull.king.web.controller.healthcheck.BaseHealthCheck
import org.skull.king.web.exception.DomainErrorExceptionMapper


class SkullKingApplication : Application<SkullKingConfig>() {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            SkullKingApplication().run(*args)
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

            healthChecks().register("base", BaseHealthCheck());

            val skullKingComponent: SkullKingComponent = DaggerSkullKingComponent.builder()
                .configurationModule(ConfigurationModule(configuration, mapper))
                .build()

            // exception mapper
            jersey().register(DomainErrorExceptionMapper())

            // controllers
            jersey().register(skullKingComponent.provideSkullKingResource())
        }
    }
}
