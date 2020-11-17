package org.skull.king

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.skull.king.component.DaggerSkullKingComponent
import org.skull.king.component.SkullKingComponent
import org.skull.king.config.SkullKingConfig
import org.skull.king.module.ConfigurationModule
import org.skull.king.web.controller.healthcheck.BaseHealthCheck
import org.skull.king.web.exception.DomainErrorExceptionMapper


class SkullKingApplication : Application<SkullKingConfig>() {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            SkullKingApplication().run(*args)
        }
    }

    override fun run(configuration: SkullKingConfig, environment: Environment) {
        environment.run {
            objectMapper.registerKotlinModule()

            healthChecks().register("base", BaseHealthCheck());

            val skullKingComponent: SkullKingComponent = DaggerSkullKingComponent.builder()
                .configurationModule(ConfigurationModule(configuration))
                .build()

            // exception mapper
            jersey().register(DomainErrorExceptionMapper())

            // controllers
            jersey().register(skullKingComponent.provideSkullKingResource())
        }
    }

}
