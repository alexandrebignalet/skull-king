package org.skull.king.query

import org.skull.king.command.Card

sealed class ReadEntity

data class ReadSkullKing(
    val id: String,
    val players: List<String>,
    val roundNb: Int,
    val fold: Map<String, Card> = mapOf()
) : ReadEntity()

data class ReadPlayer(
    val id: String,
    val gameId: String,
    val cards: List<Card>,
    val score: Score = mutableMapOf()
) : ReadEntity()

// TODO create a read model for card which might update according on card allowed or not

typealias Announced = Int
typealias RoundNb = Int
typealias Done = Int
typealias Score = MutableMap<RoundNb, Pair<Announced, Done>>

val Pair<Announced, Done>.announced get() = first
val Pair<Announced, Done>.done get() = second
