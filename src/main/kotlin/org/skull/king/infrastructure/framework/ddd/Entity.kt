package org.skull.king.infrastructure.framework.ddd

interface Entity<TId> {

    fun getId(): TId
}
