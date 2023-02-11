package org.skull.king.domain.core.command.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class GameConfigurationTest {

    @Test
    fun equality() {
        Assertions.assertThat(ClassicConfiguration).isEqualTo(ClassicConfiguration)
    }
}