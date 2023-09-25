package org.skull.king.core.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.skull.king.game_room.domain.Configuration

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "variant",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ClassicConfiguration::class, name = GameConfiguration.CLASSIC),
    JsonSubTypes.Type(value = BlackRockConfiguration::class, name = GameConfiguration.BLACKROCK),
)
sealed class GameConfiguration(val variant: String) {
    companion object {
        const val CLASSIC = "CLASSIC"
        const val BLACKROCK = "BLACKROCK"

        fun from(configuration: Configuration?) =
            configuration?.let { BlackRockConfiguration.from(it) } ?: ClassicConfiguration
    }
}

object ClassicConfiguration : GameConfiguration(CLASSIC) {

    override fun equals(other: Any?): Boolean {
        return other is ClassicConfiguration
    }

}

data class BlackRockConfiguration(
    val kraken: Boolean,
    val whale: Boolean,
    val butins: Boolean
) : GameConfiguration(BLACKROCK) {

    companion object {

        fun from(gameRoomConfiguration: Configuration) = BlackRockConfiguration(
            kraken = gameRoomConfiguration.withKraken,
            whale = gameRoomConfiguration.withWhale,
            butins = gameRoomConfiguration.withButins,
        )

    }
}

