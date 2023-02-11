package org.skull.king.infrastructure.framework.saga

import org.skull.king.infrastructure.framework.command.Command

interface Saga<TResponse> : Command<TResponse>
