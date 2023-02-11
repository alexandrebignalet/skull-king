package org.skull.king.domain.core.command.service

import org.skull.king.domain.core.command.domain.BlackRockConfiguration
import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.CardColor.BLACK
import org.skull.king.domain.core.command.domain.CardType
import org.skull.king.domain.core.command.domain.ClassicConfiguration
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.GameConfiguration
import org.skull.king.domain.core.command.domain.PlayerId
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.domain.core.command.domain.ScaryMaryUsage

typealias Fold = List<FoldSettlementService.PlayerCard>

object FoldSettlementService {

    data class PlayerCard(val playerId: PlayerId, val card: Card)
    data class FoldSettlement(
        val nextFoldFirstPlayerId: PlayerId,
        val potentialBonus: Int = 0,
        val won: Boolean = true
    )

    fun settleFold(configuration: GameConfiguration, foldPlayed: Map<PlayerId, Card>): FoldSettlement {
        val fold = foldPlayed.map { PlayerCard(it.key, it.value) }

        val kraken = fold.kraken()
        val skullKing = fold.skullKing()
        val pirates = fold.pirates()
        val mermaids = fold.mermaids()
        val highestBlackCard = fold.highestOf(BLACK)

        val fourteensBonuses =
            (fold.fourteenBlack()?.let { bonusResolver(configuration, BonusType.FOURTEEN_BLACK) } ?: 0) +
                    bonusResolver(configuration, BonusType.FOURTEEN_DEFAULT, fold.fourteensNotBlack().size)

        return when {
            kraken.isNotEmpty() -> {
                val foldWithoutKraken = foldPlayed.filter { it.key != kraken.first().playerId }
                return settleFold(configuration, foldWithoutKraken)
                    .copy(won = false, potentialBonus = 0)
            }

            skullKing.isNotEmpty() -> when {
                mermaids.isNotEmpty() -> FoldSettlement(
                    nextFoldFirstPlayerId = mermaids.first().playerId,
                    potentialBonus = bonusResolver(configuration, BonusType.MERMAID_OVER_SKULLKING) + fourteensBonuses
                )

                else -> FoldSettlement(
                    nextFoldFirstPlayerId = skullKing.first().playerId,
                    potentialBonus = bonusResolver(
                        configuration,
                        BonusType.SKULLKING_PER_PIRATE,
                        pirates.size
                    ) + fourteensBonuses
                )
            }

            pirates.isNotEmpty() -> FoldSettlement(
                nextFoldFirstPlayerId = pirates.first().playerId,
                potentialBonus =
                bonusResolver(configuration, BonusType.PIRATE_PER_MERMAID, mermaids.size) + fourteensBonuses
            )

            mermaids.isNotEmpty() -> FoldSettlement(mermaids.first().playerId, fourteensBonuses)
            highestBlackCard != null -> FoldSettlement(highestBlackCard.playerId, fourteensBonuses)
            fold.onlyEscapes() -> FoldSettlement(fold.first().playerId)
            else -> FoldSettlement(fold.highestOf(fold.colorAsked()!!)!!.playerId, fourteensBonuses)
        }
    }

    private fun bonusResolver(configuration: GameConfiguration, bonusType: BonusType, occurrences: Int = 1) =
        when (configuration) {
            is ClassicConfiguration -> when (bonusType) {
                BonusType.MERMAID_OVER_SKULLKING -> 50
                BonusType.SKULLKING_PER_PIRATE -> 30 * occurrences
                else -> 0
            }

            is BlackRockConfiguration -> when (bonusType) {
                BonusType.MERMAID_OVER_SKULLKING -> 40
                BonusType.SKULLKING_PER_PIRATE -> 30 * occurrences
                BonusType.PIRATE_PER_MERMAID -> 20 * occurrences
                BonusType.FOURTEEN_BLACK -> 20
                BonusType.FOURTEEN_DEFAULT -> 10 * occurrences
            }
        }

    private fun Fold.kraken() =
        filter { it.card.type == CardType.KRAKEN }

    private fun Fold.skullKing() =
        filter { it.card.type == CardType.SKULLKING }

    private fun Fold.mermaids() =
        filter { it.card.type == CardType.MERMAID }

    private fun Fold.pirates() = filter {
        it.card.type == CardType.PIRATE || (it.card is ScaryMary && it.card.usage == ScaryMaryUsage.PIRATE)
    }

    private fun Fold.onlyEscapes() = all {
        (it.card.type == CardType.ESCAPE) || (it.card is ScaryMary && it.card.usage == ScaryMaryUsage.ESCAPE)
    }

    private fun Fold.fourteensNotBlack() =
        filter { it.card is ColoredCard && it.card.color != BLACK && it.card.value == 14 }

    private fun Fold.fourteenBlack() =
        find { it.card is ColoredCard && it.card.color == BLACK && it.card.value == 14 }

    private fun Fold.highestOf(colorAsked: CardColor) =
        filter { it.card is ColoredCard && it.card.color == colorAsked }.maxByOrNull { (it.card as ColoredCard).value }

    private fun Fold.colorAsked() =
        firstOrNull { it.card is ColoredCard }?.let { (_, card) -> (card as ColoredCard).color }

}

enum class BonusType {
    MERMAID_OVER_SKULLKING,
    SKULLKING_PER_PIRATE,
    PIRATE_PER_MERMAID,
    FOURTEEN_BLACK,
    FOURTEEN_DEFAULT
}
