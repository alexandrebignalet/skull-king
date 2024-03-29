package org.skull.king.core.domain

interface QueryRepository {

    fun getGame(gameId: String): ReadSkullKing?

    fun getGamePlayers(gameId: String): List<ReadPlayer>

    fun getPlayer(gameId: String, playerId: String): ReadPlayer?

    fun registerPlayerAnnounce(gameId: String, playerScore: PlayerRoundScore, isLastAnnounce: Boolean): Result<Unit>

    fun projectFoldSettled(
        gameId: String,
        playerId: String,
        roundNb: RoundNb,
        score: Score,
        butinAllies: List<Pair<String, Score>>
    ): Result<Unit>

    fun movePlayerCardToGameFold(gameId: String, playerId: String, card: ReadCard, nextPlayerId: String): Result<Unit>

    fun saveGameAndPlayers(game: ReadSkullKing, players: List<ReadPlayer>): Result<Unit>

    fun endGame(gameId: String): Result<Unit>

    fun saveNewRound(
        gameId: String,
        nextRoundNb: Int,
        newFirstPlayerId: String,
        players: List<ReadPlayer>
    ): Result<Unit>
}
