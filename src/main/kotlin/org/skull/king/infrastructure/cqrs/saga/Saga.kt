package org.skull.king.infrastructure.cqrs.saga

import org.skull.king.infrastructure.cqrs.command.Command

interface Saga<TResponse> : Command<TResponse>
