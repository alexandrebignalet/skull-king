package org.skull.king.game_room.domain

class AlreadyInGameRoomException(userId: String, gameRoomId: String) :
    BaseGameRoomException("User $userId already in game room $gameRoomId")
