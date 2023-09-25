package org.skull.king.core.domain

object CardService {

    /**
     * Fold might be ordered as players order
     */
    fun isCardPlayAllowed(aFold: List<Card>, cardsInHand: List<Card>, target: Card): Boolean {
        if (!cardsInHand.contains(target)) return false

        val colorAsked: ColoredCard? = aFold.firstOrNull { it is ColoredCard } as ColoredCard?

        colorAsked?.let { (_, color): ColoredCard ->
            if (cardsInHand.filterIsInstance<ColoredCard>().none { it.color == color }) return true
            if (target is ColoredCard && target.color != color) return false
        }

        return true
    }
}
