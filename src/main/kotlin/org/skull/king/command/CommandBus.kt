package org.skull.king.command

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.skull.king.application.createActor
import org.skull.king.command.handler.AnnounceHandler
import org.skull.king.command.handler.PlayCardHandler
import org.skull.king.command.handler.SettleFoldHandler
import org.skull.king.command.handler.StartHandler
import org.skull.king.event.EventStore
import org.skull.king.functional.Valid

class CommandHandler(val eventStore: EventStore) {

    //if we need we can have multiple instances
    val sendChannel = createActor<CommandMsg> { executeCommand(it) }

    private fun executeCommand(msg: CommandMsg) {

        val res = processPoly(msg.command)(eventStore)

        runBlocking {
            //we want to reply after sending the event to the store
            if (res is Valid) {
                eventStore.sendChannel.send(res.value)
            }
            msg.response.complete(res)
        }
    }

    private fun processPoly(c: Command): EsScope {

        println("Processing $c")

        return when (c) {
            is StartSkullKing -> StartHandler.execute(c)
            is AnnounceWinningCardsFoldCount -> AnnounceHandler.execute(c)
            is PlayCard -> PlayCardHandler.execute(c)
            is SettleFoldWinner -> SettleFoldHandler.execute(c)
        }
    }

    fun handle(cmd: Command): CompletableDeferred<CmdResult> = runBlocking {
        //use launch to execute commands in parallel slightly out of order
        CommandMsg(cmd, CompletableDeferred()).let {
            sendChannel.send(it)
            it.response
        }
    }
}

