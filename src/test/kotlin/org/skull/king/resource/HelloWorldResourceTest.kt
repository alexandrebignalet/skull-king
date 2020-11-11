package org.skull.king.resource

import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skull.king.SkullKingApplication
import org.skull.king.config.SkullKingConfig

@ExtendWith(DropwizardExtensionsSupport::class)
class HelloWorldResourceTest {

    private val EXTENSION = DropwizardAppExtension<SkullKingConfig>(
        SkullKingApplication::class.java,
        ResourceHelpers.resourceFilePath("config.yml"),
        *configOverride()
    )

    private fun configOverride(): Array<ConfigOverride> {
        return arrayOf()
    }

    @Test
    fun `Should say hello using the test config`() {
        val name = "Skull"
        val response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/hello-world")
            .queryParam("name", name)
            .request()
            .get()
            .readEntity(String::class.java)

        Assertions.assertThat(response).isEqualTo("Hello $name, welcome to SkullAppTest")
    }
}
