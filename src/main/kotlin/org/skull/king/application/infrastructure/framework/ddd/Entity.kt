package org.skull.king.application.infrastructure.framework.ddd

interface Entity<TId> {

    fun getId(): TId
}
