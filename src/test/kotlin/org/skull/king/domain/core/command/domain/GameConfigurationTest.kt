package org.skull.king.domain.core.command.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.core.domain.ClassicConfiguration

class GameConfigurationTest {

    @Test
    fun equality() {
        Assertions.assertThat(ClassicConfiguration).isEqualTo(ClassicConfiguration)
    }
}