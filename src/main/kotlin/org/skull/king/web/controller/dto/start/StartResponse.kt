package org.skull.king.web.controller.dto.start

import com.fasterxml.jackson.annotation.JsonProperty

data class StartResponse(@JsonProperty("game_id") val gameId: String)
