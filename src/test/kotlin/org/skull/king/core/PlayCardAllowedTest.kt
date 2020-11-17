package org.skull.king.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.core.command.domain.Card
import org.skull.king.core.command.domain.CardColor
import org.skull.king.core.command.domain.ColoredCard
import org.skull.king.core.command.domain.SpecialCard
import org.skull.king.core.command.domain.SpecialCardType
import org.skull.king.core.command.service.CardService.isCardPlayAllowed

class PlayCardAllowedTest {

    @Test
    fun `Should not allow to play a card not in hand`() {
        val fold = listOf<Card>()
        Assertions.assertThat(
            isCardPlayAllowed(
                fold,
                listOf(),
                ColoredCard(1, CardColor.RED)
            )
        ).isFalse()
    }

    @Test
    fun `Should not allow to play a colored card with a different color than the asked one if one in hand`() {
        val cardAllowed = isCardPlayAllowed(
            listOf(ColoredCard(2, CardColor.RED)),
            listOf(ColoredCard(4, CardColor.RED), ColoredCard(1, CardColor.BLUE)),
            ColoredCard(1, CardColor.BLUE)
        )
        Assertions.assertThat(cardAllowed).isFalse()
    }

    @Test
    fun `Should allow to play special card anytime`() {
        val cardAllowed = isCardPlayAllowed(
            listOf(ColoredCard(2, CardColor.RED)),
            listOf(SpecialCard(SpecialCardType.PIRATE), ColoredCard(1, CardColor.BLUE)),
            SpecialCard(SpecialCardType.PIRATE)
        )
        Assertions.assertThat(cardAllowed).isTrue()
    }
}

