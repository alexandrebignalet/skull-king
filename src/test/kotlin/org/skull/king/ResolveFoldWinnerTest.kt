package org.skull.king

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.skull.king.command.Card
import org.skull.king.command.CardColor
import org.skull.king.command.ColoredCard
import org.skull.king.command.PlayerId
import org.skull.king.command.ScaryMary
import org.skull.king.command.ScaryMaryUsage
import org.skull.king.command.SpecialCard
import org.skull.king.command.SpecialCardType
import org.skull.king.command.settleFoldWinner
import java.util.stream.Stream

class ResolveFoldWinnerTest {

    @ParameterizedTest
    @ArgumentsSource(FoldProvider::class)
    fun `Should resolve the expected winner`(fold: Map<PlayerId, Card>, expectedWinner: PlayerId) {
        Assertions.assertThat(settleFoldWinner(fold)).isEqualTo(expectedWinner)
    }

    private class FoldProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), "2"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE),
                    "4" to ColoredCard(8, CardColor.BLUE),
                    "5" to ColoredCard(9, CardColor.YELLOW),
                    "6" to ColoredCard(10, CardColor.BLUE)
                ), "2"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.BLACK),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(2, CardColor.BLUE),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.YELLOW),
                    "2" to ColoredCard(7, CardColor.YELLOW),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), "2"
            ),
            // HIGHEST BLACK WINS VERSUS COLORED
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), "3"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), "2"
            ),

            // ESCAPE SHOULD NEVER WINS
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK),
                    "4" to SpecialCard(SpecialCardType.ESCAPE)
                ), "2"
            ),
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.ESCAPE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), "2"
            ),
            // IF ONLY ESCAPES PLAYED THEN FIRST PLAYER WINS
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.ESCAPE),
                    "2" to SpecialCard(SpecialCardType.ESCAPE),
                    "3" to SpecialCard(SpecialCardType.ESCAPE),
                    "4" to ScaryMary(ScaryMaryUsage.ESCAPE)
                ), "1"
            ),
            // PIRATE WINS AGAINST ANY COLORED
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.PIRATE),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to SpecialCard(SpecialCardType.ESCAPE),
                    "4" to ColoredCard(5, CardColor.RED)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE)
                ), "3"
            ),
            // FIRST PIRATE PLAYED WINS
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.PIRATE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE)
                ), "1"
            ),
            // MERMAID WINS OVER COLORED CARD
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.MERMAID),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to SpecialCard(SpecialCardType.ESCAPE),
                    "4" to ColoredCard(5, CardColor.RED)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.MERMAID)
                ), "3"
            ),
            // FIRST MERMAID PLAYED WINS (if no pirates or skull)
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.MERMAID),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.MERMAID)
                ), "1"
            ),
            // MERMAID LOOSE AGAINST PIRATE
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.PIRATE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.MERMAID)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.MERMAID),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE)
                ), "3"
            ),
            // SKULLKING WINS AGAINST COLORED CARD
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.SKULL_KING),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to SpecialCard(SpecialCardType.ESCAPE),
                    "4" to ColoredCard(5, CardColor.RED)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.SKULL_KING)
                ), "3"
            ),
            // SKULLKING WINS AGAINST MERMAID
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.SKULL_KING),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE),
                    "4" to SpecialCard(SpecialCardType.PIRATE)
                ), "1"
            ),
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.MERMAID),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE)
                ), "3"
            ),
            // SKULLKING LOOSE AGAINST MERMAID
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.SKULL_KING),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE),
                    "4" to SpecialCard(SpecialCardType.MERMAID)
                ), "4"
            ),
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.MERMAID),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE)
                ), "3"
            ),
            // SCARY MARY LOSE IF ESCAPED
            Arguments.of(
                mapOf(
                    "1" to ScaryMary(ScaryMaryUsage.ESCAPE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SpecialCard(SpecialCardType.PIRATE)
                ), "3"
            ),
            // SCARY MARY IS A PIRATE IF PIRATE
            Arguments.of(
                mapOf(
                    "1" to SpecialCard(SpecialCardType.MERMAID),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ScaryMary(ScaryMaryUsage.PIRATE)
                ), "3"
            )
        )
    }
}
