package org.skull.king.domain.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Escape
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.PlayerId
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.domain.core.command.domain.ScaryMaryUsage
import org.skull.king.domain.core.command.domain.SkullKingCard
import org.skull.king.domain.core.command.service.FoldSettlementService.FoldSettlement
import org.skull.king.domain.core.command.service.FoldSettlementService.settleFoldWinner
import java.util.stream.Stream

class ResolveFoldWinnerTest {

    @ParameterizedTest
    @ArgumentsSource(FoldProvider::class)
    fun `Should resolve the expected winner`(fold: Map<PlayerId, Card>, settlement: FoldSettlement) {
        Assertions.assertThat(settleFoldWinner(fold)).isEqualTo(settlement)
    }

    private class FoldProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("2")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE),
                    "4" to ColoredCard(8, CardColor.BLUE),
                    "5" to ColoredCard(9, CardColor.YELLOW),
                    "6" to ColoredCard(10, CardColor.BLUE)
                ), FoldSettlement("2")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.BLACK),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(2, CardColor.BLUE),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.YELLOW),
                    "2" to ColoredCard(7, CardColor.YELLOW),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("2")
            ),
            // HIGHEST BLACK WINS VERSUS COLORED
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("3")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("2")
            ),

            // ESCAPE SHOULD NEVER WINS
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK),
                    "4" to Escape()
                ), FoldSettlement("2")
            ),
            Arguments.of(
                mapOf(
                    "1" to Escape(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("2")
            ),
            // IF ONLY ESCAPES PLAYED THEN FIRST PLAYER WINS
            Arguments.of(
                mapOf(
                    "1" to Escape(),
                    "2" to Escape(),
                    "3" to Escape(),
                    "4" to ScaryMary(ScaryMaryUsage.ESCAPE)
                ), FoldSettlement("1")
            ),
            // PIRATE WINS AGAINST ANY COLORED
            Arguments.of(
                mapOf(
                    "1" to Pirate(PirateName.HARRY_THE_GIANT),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to Escape(),
                    "4" to ColoredCard(5, CardColor.RED)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT)
                ), FoldSettlement("3")
            ),
            // FIRST PIRATE PLAYED WINS
            Arguments.of(
                mapOf(
                    "1" to Pirate(PirateName.HARRY_THE_GIANT),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.BETTY_BRAVE)
                ), FoldSettlement("1")
            ),
            // MERMAID WINS OVER COLORED CARD
            Arguments.of(
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to Escape(),
                    "4" to ColoredCard(5, CardColor.RED)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("3")
            ),
            // FIRST MERMAID PLAYED WINS (if no pirates or skull)
            Arguments.of(
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("1")
            ),
            // MERMAID LOOSE AGAINST PIRATE
            Arguments.of(
                mapOf(
                    "1" to Pirate(PirateName.BETTY_BRAVE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("1")
            ),
            Arguments.of(
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.BETTY_BRAVE)
                ), FoldSettlement("3")
            ),
            // SKULLKING WINS AGAINST COLORED CARD
            Arguments.of(
                mapOf(
                    "1" to SkullKingCard(),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to Escape(),
                    "4" to ColoredCard(5, CardColor.RED)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SkullKingCard()
                ), FoldSettlement("3")
            ),
            // SKULLKING WINS AGAINST PIRATES
            Arguments.of(
                mapOf(
                    "1" to SkullKingCard(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.BETTY_BRAVE),
                    "4" to Pirate(PirateName.HARRY_THE_GIANT)
                ), FoldSettlement("1", 60)
            ),
            Arguments.of(
                mapOf(
                    "1" to Pirate(PirateName.HARRY_THE_GIANT),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SkullKingCard()
                ), FoldSettlement("3", 30)
            ),
            // SKULLKING LOOSE AGAINST MERMAID
            Arguments.of(
                mapOf(
                    "1" to SkullKingCard(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT),
                    "4" to Mermaid()
                ), FoldSettlement("4", 50)
            ),
            Arguments.of(
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SkullKingCard()
                ), FoldSettlement("1", 50)
            ),
            // SCARY MARY LOSE IF ESCAPED
            Arguments.of(
                mapOf(
                    "1" to ScaryMary(ScaryMaryUsage.ESCAPE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT)
                ), FoldSettlement("3")
            ),
            // SCARY MARY IS A PIRATE IF PIRATE
            Arguments.of(
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ScaryMary(ScaryMaryUsage.PIRATE)
                ), FoldSettlement("3")
            )
        )
    }
}
