package org.skull.king.core.infrastructure.web

import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class AnnounceWinningCardsFoldCountRequest(
    @get:Min(0)
    @get:Max(10)
    val count: Int
)
