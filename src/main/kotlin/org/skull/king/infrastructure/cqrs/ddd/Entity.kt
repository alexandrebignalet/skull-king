package org.skull.king.infrastructure.cqrs.ddd

interface Entity<TId> {

    fun getId(): TId
}
