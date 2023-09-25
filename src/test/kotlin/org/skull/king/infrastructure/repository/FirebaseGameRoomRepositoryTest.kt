package org.skull.king.infrastructure.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.application.utils.JsonObjectMapper
import org.skull.king.game_room.domain.GameRoom
import org.skull.king.game_room.domain.GameUser
import org.skull.king.game_room.infrastructure.repository.FirebaseGameRoomRepository
import org.skull.king.helpers.LocalFirebase

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
