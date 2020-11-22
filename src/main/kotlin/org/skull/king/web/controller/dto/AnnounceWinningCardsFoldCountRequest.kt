package org.skull.king.web.controller.dto

import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class AnnounceWinningCardsFoldCountRequest(
    @get:Min(0)
    @get:Max(10)
    val count: Int
)
