package org.skull.king.domain.supporting.room.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class Configuration(
    @JsonProperty("with_kraken")
    val withKraken: Boolean,
    @JsonProperty("with_whale")
    val withWhale: Boolean
) {

    fun fireMap() = mapOf(
        "with_kraken" to withKraken,
        "with_whale" to withWhale
    )
}