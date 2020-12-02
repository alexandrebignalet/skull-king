package org.skull.king.domain.core.command.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.Stack
import java.util.UUID

enum class CardType {
    ESCAPE,
    MERMAID,
    COLORED,
    PIRATE,
    SCARY_MARY,
    SKULLKING
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ColoredCard::class, name = "COLORED"),
    JsonSubTypes.Type(value = SkullKingCard::class, name = "SKULLKING"),
    JsonSubTypes.Type(value = ScaryMary::class, name = "SCARY_MARY"),
    JsonSubTypes.Type(value = Pirate::class, name = "PIRATE"),
    JsonSubTypes.Type(value = Escape::class, name = "ESCAPE"),
    JsonSubTypes.Type(value = Mermaid::class, name = "MERMAID")
)
abstract class Card(val type: CardType, val id: String = UUID.randomUUID().toString()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Card

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}

data class ColoredCard(val value: Int, val color: CardColor) : Card(CardType.COLORED)
enum class CardColor { RED, BLUE, YELLOW, BLACK }

enum class PirateName {
    HARRY_THE_GIANT,
    TORTUGA_JACK,
    EVIL_EMMY,
    BADEYE_JOE,
    BETTY_BRAVE
}

data class Pirate(val name: PirateName) : Card(CardType.PIRATE)
class SkullKingCard : Card(CardType.SKULLKING)
class Mermaid : Card(CardType.MERMAID)
class Escape : Card(CardType.ESCAPE)

enum class ScaryMaryUsage { ESCAPE, PIRATE, NOT_SET }
class ScaryMary(val usage: ScaryMaryUsage = ScaryMaryUsage.NOT_SET) : Card(CardType.SCARY_MARY) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScaryMary) return false
        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class Deck(val cards: List<Card> = SkullKing.CARDS) {
    private val deck = cards.shuffled().fold(Stack<Card>(), { acc, s -> acc.push(s); acc })

    fun pop(): Card = deck.pop()
}
