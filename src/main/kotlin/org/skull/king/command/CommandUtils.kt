package org.skull.king.command

import kotlinx.coroutines.CompletableDeferred
import org.skull.king.command.error.DomainError
import org.skull.king.event.Event
import org.skull.king.event.EventStore
import org.skull.king.functional.Validated

typealias CmdResult = Validated<DomainError, List<Event>>
typealias EsScope = EventStore.() -> CmdResult

data class CommandMsg(val command: Command, val response: CompletableDeferred<CmdResult>) // a command with a result
