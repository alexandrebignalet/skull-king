package org.skull.king.core.domain

data class AnnounceState(
    val gameId: String,
    val players: List<Player>,
    val roundNb: Int,
    val configuration: GameConfiguration,
    val version: Int
) : Skullking(gameId) {

    @Suppress("UNCHECKED_CAST")
    override fun compose(e: SkullKingEvent, version: Int) = when (e) {
        is PlayerAnnounced -> {
            val updatedPlayers = players.map {
                if (it.id == e.playerId) ReadyPlayer(it.id, gameId, (it as NewPlayer).cards, e.count)
                else it
            }

            val allPlayersAnnounced = updatedPlayers.all { it is ReadyPlayer }
            if (!allPlayersAnnounced) {
                AnnounceState(gameId, updatedPlayers, roundNb, configuration, version)
            } else {
                RoundState(
                    gameId,
                    updatedPlayers.filterIsInstance<ReadyPlayer>(),
                    roundNb,
                    firstPlayerId = players.first().id,
                    configuration = configuration,
                    version = version
                )
            }
        }

        else -> this
    }

    override fun playCard(playerId: String, card: Card): CardPlayed {
        throw SkullKingNotReadyError(
            "All players must announce before starting to play cards",
            this
        )
    }

    fun hasAlreadyAnnounced(playerId: String) = players.any {
        it.id == playerId && it is ReadyPlayer
    }

    fun has(playerId: String) = players.any { it.id == playerId }

    fun isMissingOneLastAnnounce(): Boolean {
        return players.filterIsInstance<ReadyPlayer>().count() == players.count() - 1
    }
}