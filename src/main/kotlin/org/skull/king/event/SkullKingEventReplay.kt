package org.skull.king.event

import org.skull.king.command.domain.SkullKing
import org.skull.king.command.domain.emptySkullKing

fun List<SkullKingEvent>.fold(): SkullKing {
    return this.fold(emptySkullKing) { i: SkullKing, e: SkullKingEvent -> i.compose(e) }
}
