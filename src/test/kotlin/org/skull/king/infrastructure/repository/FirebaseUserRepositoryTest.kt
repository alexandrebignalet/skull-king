package org.skull.king.infrastructure.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.supporting.room.domain.GameRoom
import org.skull.king.domain.supporting.user.domain.GameUser
import org.skull.king.helpers.LocalFirebase
import org.skull.king.utils.JsonObjectMapper

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

        Assertions.assertThat(result).isEqualToIgnoringGivenFields(bot)
    }
}
