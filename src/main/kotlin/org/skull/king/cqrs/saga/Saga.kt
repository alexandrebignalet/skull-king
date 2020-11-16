package org.skull.king.cqrs.saga

import org.skull.king.cqrs.command.Command

interface Saga<TResponse> : Command<TResponse>
