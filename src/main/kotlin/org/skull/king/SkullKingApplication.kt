package org.skull.king

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.skull.king.config.SkullKingConfig
import org.skull.king.resource.HelloWorldResource
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
            healthChecks().register("base", BaseHealthCheck());

            jersey().register(HelloWorldResource(configuration.appName))
        }
    }

}
