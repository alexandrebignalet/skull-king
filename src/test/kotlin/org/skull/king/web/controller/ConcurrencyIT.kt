package org.skull.king.web.controller

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.mockk.every
import io.mockk.mockkConstructor
import java.util.*
import kotlin.concurrent.thread
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.extension.ExtendWith
import org.skull.king.SkullkingApplication
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.SkullkingCard
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.helpers.ApiHelper
import org.skull.king.helpers.LocalBus
import org.skull.king.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.infrastructure.authentication.User
import org.skull.king.web.controller.dto.start.StartResponse

@ExtendWith(DropwizardExtensionsSupport::class)
class ConcurrencyIT : LocalBus() {
    companion object {

        private val userOne = User("1", "johnny", "uid@example.com")
        private val userTwo = User("2", "johnny", "uid@example.com")
        private val userThree = User("3", "johnny", "uid@example.com")
        private val userFour = User("4", "johnny", "uid@example.com")
        private val userFive = User("5", "johnny", "uid@example.com")
        private val userSix = User("6", "johnny", "uid@example.com")
        private val users = listOf(
            userOne,
            userTwo,
            userThree,
            userFour,
            userFive,
            userSix
        )

        @JvmStatic
        @BeforeAll
        fun mockAuthentication() {
            mockkConstructor(FirebaseAuthenticator::class)
            users.forEach { every { anyConstructed<FirebaseAuthenticator>().authenticate(it.id) } returns Optional.of(it) }
        }
    }

    private val EXTENSION =
        DropwizardAppExtension(SkullkingApplication::class.java, ResourceHelpers.resourceFilePath("config.yml"))
    private val mockedCard = listOf(
        Mermaid(),
        SkullkingCard(),
        Pirate(PirateName.EVIL_EMMY),
        Pirate(PirateName.HARRY_THE_GIANT),
        Pirate(PirateName.TORTUGA_JACK),
        Mermaid(),

        ColoredCard(3, CardColor.BLUE),
        ColoredCard(8, CardColor.BLUE),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(5, CardColor.RED)
    )
    private val players = listOf("1", "2", "3", "4", "5", "6")
    private val api = ApiHelper(EXTENSION)

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @RepeatedTest(10)
    fun `Should handle correctly concurrent announcement as sequential announcement`() {
        val (gameId) = api.skullKing.start(users).readEntity(StartResponse::class.java)

        val threads = players.map { playerId ->
            thread { api.skullKing.announce(gameId, playerId, 1, idToken = playerId) }
        }

        threads.forEach { it.join() }

        val game = queryBus.send(GetGame(gameId))
        Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.CARDS)
        Assertions.assertThat(game.scoreBoard).hasSize(players.size)
    }
}
