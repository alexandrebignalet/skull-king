package org.skull.king.core.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.skull.king.core.command.domain.Card
import org.skull.king.core.command.domain.NewPlayer
import org.skull.king.core.command.domain.Player
import org.skull.king.core.command.domain.PlayerId
import org.skull.king.cqrs.ddd.event.Event
import java.time.Instant

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Started::class, name = Started.EVENT_TYPE),
    JsonSubTypes.Type(value = PlayerAnnounced::class, name = PlayerAnnounced.EVENT_TYPE),
    JsonSubTypes.Type(value = CardPlayed::class, name = CardPlayed.EVENT_TYPE),
    JsonSubTypes.Type(value = FoldWinnerSettled::class, name = FoldWinnerSettled.EVENT_TYPE),
    JsonSubTypes.Type(value = NewRoundStarted::class, name = NewRoundStarted.EVENT_TYPE),
    JsonSubTypes.Type(value = GameFinished::class, name = GameFinished.EVENT_TYPE)
)
open class SkullKingEvent(
    override val aggregateId: String,
    override val type: String,
    override val version: Int = 1,
    override val aggregateType: String = SKULLKING_AGGREGATE_TYPE,
    override val timestamp: Long = Instant.now().toEpochMilli()
) : Event {
    companion object {
        const val SKULLKING_AGGREGATE_TYPE = "SKULLKING"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkullKingEvent

        if (aggregateId != other.aggregateId) return false
        if (type != other.type) return false
        if (version != other.version) return false
        if (aggregateType != other.aggregateType) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + version
        result = 31 * result + aggregateType.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }


}

data class Started(val gameId: String, val players: List<Player>) : SkullKingEvent(gameId, EVENT_TYPE) {
    companion object {
        const val EVENT_TYPE = "game_started"
    }
}

data class PlayerAnnounced(
    val gameId: String,
    val playerId: String,
    val count: Int,
    val roundNb: Int
) : SkullKingEvent(gameId, EVENT_TYPE) {
    companion object {
        const val EVENT_TYPE = "player_announced"
    }
}

data class CardPlayed(
    val gameId: String,
    val playerId: String,
    val card: Card,
    val isLastFoldPlay: Boolean = false
) : SkullKingEvent(gameId, EVENT_TYPE) {
    companion object {
        const val EVENT_TYPE = "card_played"
    }
}

data class FoldWinnerSettled(
    val gameId: String,
    val winner: PlayerId,
    val potentialBonus: Int
) :
    SkullKingEvent(gameId, EVENT_TYPE) {
    companion object {
        const val EVENT_TYPE = "fold_winner_settled"
    }
}

data class NewRoundStarted(
    val gameId: String,
    val nextRoundNb: Int,
    val players: List<NewPlayer>
) :
    SkullKingEvent(gameId, EVENT_TYPE) {
    companion object {
        const val EVENT_TYPE = "new_round_finished"
    }
}

data class GameFinished(val gameId: String) : SkullKingEvent(gameId, EVENT_TYPE) {
    companion object {
        const val EVENT_TYPE = "game_finished"
    }
}
