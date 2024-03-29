package org.skull.king.domain.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.skull.king.core.domain.*
import org.skull.king.core.infrastructure.FoldSettlementService.FoldSettlement
import org.skull.king.core.infrastructure.FoldSettlementService.settleFold
import java.util.stream.Stream

class FoldSettlementServiceTest {

    @ParameterizedTest
    @ArgumentsSource(FoldProvider::class)
    fun `Should resolve the expected winner`(
        configuration: GameConfiguration,
        fold: Map<PlayerId, Card>,
        settlement: FoldSettlement
    ) {
        Assertions.assertThat(settleFold(configuration, fold)).isEqualTo(settlement)
    }

    private class FoldProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> = Stream.of(
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("2")
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to ColoredCard(1, CardColor.PURPLE),
                    "2" to ColoredCard(7, CardColor.PURPLE),
                    "3" to ColoredCard(1, CardColor.GREEN)
                ), FoldSettlement("2")
            ),
            Arguments.of(
                ClassicConfiguration,
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
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.PURPLE),
                    "2" to ColoredCard(7, CardColor.PURPLE),
                    "3" to ColoredCard(1, CardColor.GREEN),
                    "4" to ColoredCard(8, CardColor.GREEN),
                    "5" to ColoredCard(9, CardColor.YELLOW),
                    "6" to ColoredCard(10, CardColor.GREEN)
                ), FoldSettlement("2")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.BLACK),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("3")
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to ColoredCard(1, CardColor.PURPLE),
                    "2" to ColoredCard(7, CardColor.PURPLE),
                    "3" to ColoredCard(14, CardColor.BLACK)
                ), FoldSettlement("3", 20)
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(2, CardColor.BLUE),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.YELLOW),
                    "2" to ColoredCard(7, CardColor.YELLOW),
                    "3" to ColoredCard(1, CardColor.BLUE)
                ), FoldSettlement("2")
            ),
            // HIGHEST BLACK WINS VERSUS COLORED
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("3")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("2")
            ),

            // ESCAPE SHOULD NEVER WINS
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(1, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK),
                    "4" to Escape()
                ), FoldSettlement("2")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Escape(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("2")
            ),
            // IF ONLY ESCAPES PLAYED THEN FIRST PLAYER WINS
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Escape(),
                    "2" to Escape(),
                    "3" to Escape(),
                    "4" to ScaryMary(ScaryMaryUsage.ESCAPE)
                ), FoldSettlement("1", 0, false, listOf())
            ),
            // PIRATE WINS AGAINST ANY COLORED
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Pirate(PirateName.HARRY_THE_GIANT),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to Escape(),
                    "4" to ColoredCard(5, CardColor.RED)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT)
                ), FoldSettlement("3")
            ),
            // FIRST PIRATE PLAYED WINS
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Pirate(PirateName.HARRY_THE_GIANT),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.BETTY_BRAVE)
                ), FoldSettlement("1")
            ),
            // MERMAID WINS OVER COLORED CARD
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to Escape(),
                    "4" to ColoredCard(5, CardColor.RED)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("3")
            ),
            // FIRST MERMAID PLAYED WINS (if no pirates or skull)
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("1")
            ),
            // MERMAID LOOSE AGAINST PIRATE
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Pirate(PirateName.BETTY_BRAVE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("1")
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to Pirate(PirateName.BETTY_BRAVE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Mermaid()
                ), FoldSettlement("1", 20)
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to Mermaid(),
                    "2" to Pirate(PirateName.BETTY_BRAVE),
                    "3" to Mermaid()
                ), FoldSettlement("2", 40)
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.BETTY_BRAVE)
                ), FoldSettlement("3")
            ),
            // SKULLKING WINS AGAINST COLORED CARD
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to SkullkingCard(),
                    "2" to ColoredCard(7, CardColor.RED),
                    "3" to Escape(),
                    "4" to ColoredCard(5, CardColor.RED)
                ), FoldSettlement("1")
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ColoredCard(7, CardColor.RED),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SkullkingCard()
                ), FoldSettlement("3")
            ),
            // SKULLKING WINS AGAINST PIRATES
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to SkullkingCard(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.BETTY_BRAVE),
                    "4" to Pirate(PirateName.HARRY_THE_GIANT)
                ), FoldSettlement("1", 60)
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Pirate(PirateName.HARRY_THE_GIANT),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SkullkingCard()
                ), FoldSettlement("3", 30)
            ),
            // SKULLKING LOOSE AGAINST MERMAID
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to SkullkingCard(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT),
                    "4" to Mermaid()
                ), FoldSettlement("4", 50)
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to SkullkingCard(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT),
                    "4" to Mermaid()
                ), FoldSettlement("4", 40)
            ),
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to SkullkingCard()
                ), FoldSettlement("1", 50)
            ),
            // SCARY MARY LOSE IF ESCAPED
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to ScaryMary(ScaryMaryUsage.ESCAPE),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to Pirate(PirateName.HARRY_THE_GIANT)
                ), FoldSettlement("3")
            ),
            // SCARY MARY IS A PIRATE IF PIRATE
            Arguments.of(
                ClassicConfiguration,
                mapOf(
                    "1" to Mermaid(),
                    "2" to ColoredCard(7, CardColor.BLACK),
                    "3" to ScaryMary(ScaryMaryUsage.PIRATE)
                ), FoldSettlement("3")
            ),
            // KRAKEN WIPE EVERYTHING
            Arguments.of(
                BlackRockConfiguration(kraken = true, whale = true, butins = false),
                mapOf(
                    "1" to Kraken(),
                    "2" to SkullkingCard(),
                    "3" to ScaryMary(ScaryMaryUsage.PIRATE)
                ), FoldSettlement("2", won = false)
            ),

            // WHITE WHALE make special useless
            Arguments.of(
                BlackRockConfiguration(kraken = true, whale = true, butins = false),
                mapOf(
                    "1" to WhiteWhale(),
                    "2" to SkullkingCard(),
                    "3" to ScaryMary(ScaryMaryUsage.PIRATE)
                ), FoldSettlement("1", won = false)
            ),

            // WHITE WHALE highest value card wins
            Arguments.of(
                BlackRockConfiguration(kraken = true, whale = true, butins = false),
                mapOf(
                    "1" to WhiteWhale(),
                    "2" to SkullkingCard(),
                    "3" to ColoredCard(1, CardColor.PURPLE)
                ), FoldSettlement("3", won = true)
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to WhiteWhale(),
                    "2" to SkullkingCard(),
                    "3" to ColoredCard(2, CardColor.PURPLE),
                    "4" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("3", won = true)
            ),
            // WHITE WHALE on card value equality - first wins
            Arguments.of(
                BlackRockConfiguration(kraken = true, whale = true, butins = false),
                mapOf(
                    "1" to WhiteWhale(),
                    "2" to SkullkingCard(),
                    "3" to ColoredCard(1, CardColor.PURPLE),
                    "4" to ColoredCard(1, CardColor.YELLOW)
                ), FoldSettlement("3", won = true)
            ),

            // WHITE WHALE apply effect when played after kraken
            Arguments.of(
                BlackRockConfiguration(kraken = true, whale = true, butins = false),
                mapOf(
                    "1" to Kraken(),
                    "2" to WhiteWhale(),
                    "3" to ColoredCard(1, CardColor.PURPLE),
                    "4" to ColoredCard(2, CardColor.RED)
                ), FoldSettlement("4", won = true)
            ),
            // KRAKEN apply effect when played after white whale
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = false),
                mapOf(
                    "1" to WhiteWhale(),
                    "2" to Kraken(),
                    "3" to ColoredCard(2, CardColor.PURPLE),
                    "4" to ColoredCard(1, CardColor.BLACK)
                ), FoldSettlement("4", won = false)
            ),
            // BUTIN win against escapes or buttin
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = true),
                mapOf(
                    "1" to Butin(),
                    "2" to Escape(),
                    "3" to Escape(),
                ), FoldSettlement("1", won = true)
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = true),
                mapOf(
                    "1" to Butin(),
                    "2" to Escape(),
                    "3" to Butin(),
                ), FoldSettlement("1", won = true)
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = true),
                mapOf(
                    "1" to Kraken(),
                    "2" to Butin(),
                    "3" to Escape(),
                ), FoldSettlement("2", won = false, potentialBonus = 0)
            ),
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = true),
                mapOf(
                    "1" to WhiteWhale(),
                    "2" to Butin(),
                    "3" to Escape(),
                ), FoldSettlement("1", won = false, potentialBonus = 0)
            ),

            // BUTIN loose against everything but mark 2 players with 20 butinPotentialBonus
            Arguments.of(
                BlackRockConfiguration(kraken = false, whale = false, butins = true),
                mapOf(
                    "1" to Butin(),
                    "2" to SkullkingCard(),
                    "3" to Escape(),
                ), FoldSettlement("2", won = true, potentialBonus = 20, butinAllies = listOf("1"))
            )
        )
    }
}
