package org.skull.king.cqrs.ddd

interface Entity<TId> {

    fun getId(): TId
}
