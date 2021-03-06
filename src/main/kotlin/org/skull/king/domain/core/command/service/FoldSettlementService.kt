package org.skull.king.domain.core.command.service

import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.CardType
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.PlayerId
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.domain.core.command.domain.ScaryMaryUsage

typealias Fold = List<FoldSettlementService.PlayerCard>

object FoldSettlementService {
    private const val MERMAID_SKULLKING_BONUS = 50
    private const val SKULLKING_BONUS_PER_PIRATE = 30

    data class PlayerCard(val playerId: PlayerId, val card: Card)
    data class FoldSettlement(val winner: PlayerId, val potentialBonus: Int = 0)


    fun settleFoldWinner(foldPlayed: Map<PlayerId, Card>): FoldSettlement {
        val fold = foldPlayed.map { PlayerCard(it.key, it.value) }

        val skullKing = fold.skullKing()
        val pirates = fold.pirates()
        val mermaids = fold.mermaids()
        val highestBlackCard = fold.highestOf(CardColor.BLACK)

        return when {
            skullKing.isNotEmpty() -> when {
                mermaids.isNotEmpty() -> FoldSettlement(mermaids.first().playerId, MERMAID_SKULLKING_BONUS)
                else -> FoldSettlement(skullKing.first().playerId, pirates.size * SKULLKING_BONUS_PER_PIRATE)
            }
            pirates.isNotEmpty() -> FoldSettlement(pirates.first().playerId)
            mermaids.isNotEmpty() -> FoldSettlement(mermaids.first().playerId)
            highestBlackCard != null -> FoldSettlement(highestBlackCard.playerId)
            fold.onlyEscapes() -> FoldSettlement(fold.first().playerId)
            else -> FoldSettlement(requireNotNull(fold.highestOf(requireNotNull(fold.colorAsked()))).playerId)
        }
    }

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

    private fun Fold.highestOf(colorAsked: CardColor) =
        filter { it.card is ColoredCard && it.card.color == colorAsked }.maxByOrNull { (it.card as ColoredCard).value }

    private fun Fold.colorAsked() =
        firstOrNull { it.card is ColoredCard }?.let { (_, card) -> (card as ColoredCard).color }

}
