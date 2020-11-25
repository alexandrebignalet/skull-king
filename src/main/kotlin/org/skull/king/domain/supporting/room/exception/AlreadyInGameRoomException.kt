package org.skull.king.domain.supporting.room.exception

class AlreadyInGameRoomException(userId: String, gameRoomId: String) :
    BaseGameRoomException("User $userId already in game room $gameRoomId")
