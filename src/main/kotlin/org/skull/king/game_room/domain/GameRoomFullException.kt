package org.skull.king.game_room.domain

class GameRoomFullException(gameRoomId: String) : BaseGameRoomException("Game room $gameRoomId is full")
