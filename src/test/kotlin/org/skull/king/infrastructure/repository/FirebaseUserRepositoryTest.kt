package org.skull.king.infrastructure.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.application.utils.JsonObjectMapper
import org.skull.king.game_room.domain.GameRoom
import org.skull.king.game_room.domain.GameUser
import org.skull.king.game_room.infrastructure.repository.FirebaseGameRoomRepository
import org.skull.king.game_room.infrastructure.repository.FirebaseUserRepository
import org.skull.king.helpers.LocalFirebase

class FirebaseUserRepositoryTest : LocalFirebase() {
    companion object {
        val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    private val gameRoomRepository = FirebaseGameRoomRepository(database, objectMapper)
    private val userGameRoomRepository = FirebaseUserRepository(database, objectMapper)

    @Test
    fun `should correctly save and retrieve bot`() {
        val bot = GameUser.bot()
        val room = GameRoom(
            creator = "moi",
            users = setOf(bot)
        )

        gameRoomRepository.save(room)

        val result = userGameRoomRepository.findOne(bot.id)

        Assertions.assertThat(result?.copy(rooms = setOf())).isEqualToIgnoringGivenFields(bot)
    }
}
