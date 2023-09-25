package org.skull.king.infrastructure.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.SkullkingCard
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.domain.supporting.room.domain.Configuration
import org.skull.king.domain.supporting.room.domain.GameRoom
import org.skull.king.domain.supporting.user.domain.GameUser
import org.skull.king.helpers.LocalFirebase
import org.skull.king.utils.JsonObjectMapper
import java.util.UUID

class FirebaseGameRoomRepositoryTest : LocalFirebase() {
    companion object {
        val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    private val repository = FirebaseGameRoomRepository(database, objectMapper)

    @Test
    fun `should correctly save and retrieve bot`() {
        val bot = GameUser.bot()
        val room = GameRoom(
            creator = "moi",
            users = setOf(bot)
        )

        repository.save(room)

        val result = repository.findOne(room.id)

        Assertions.assertThat(result).isEqualToIgnoringGivenFields(room, "updateDate")
    }
}
