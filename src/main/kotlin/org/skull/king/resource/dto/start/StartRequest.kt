package org.skull.king.resource.dto.start

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotEmpty

data class StartRequest(
    @NotEmpty
    @JsonProperty("player_ids")
    val playerIds: Set<String>
)
