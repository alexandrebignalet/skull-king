package org.skull.king

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.skull.king.component.DaggerSkullKingComponent
import org.skull.king.component.SkullKingComponent
import org.skull.king.config.SkullKingConfig
import org.skull.king.module.ConfigurationModule
import org.skull.king.resource.healthcheck.BaseHealthCheck


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

            jersey().register(skullKingComponent.provideHelloWorldResource())
            jersey().register(skullKingComponent.provideGameResource())
            jersey().register(skullKingComponent.provideSkullKingResource())
        }
    }

}
