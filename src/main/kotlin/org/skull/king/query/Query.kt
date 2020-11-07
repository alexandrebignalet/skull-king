package org.skull.king.query

sealed class Query

//object GetAllOpenOrders: Query()
//data class GetOrder(val phoneNum: String): Query()
//object GetBiggestOrder: Query()
//
//data class GetItem(val itemId: String): Query()
//object GetAllActiveItems: Query()

data class GetGame(val gameId: String) : Query()
data class GetPlayer(val gameId: String, val playerId: String) : Query()
