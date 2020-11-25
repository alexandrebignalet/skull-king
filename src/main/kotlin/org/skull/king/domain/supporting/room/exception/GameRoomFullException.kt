package org.skull.king.domain.supporting.room.exception

class GameRoomFullException(gameRoomId: String) : BaseGameRoomException("Game room $gameRoomId is full")
