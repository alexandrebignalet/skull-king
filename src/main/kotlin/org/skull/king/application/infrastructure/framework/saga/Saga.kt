package org.skull.king.application.infrastructure.framework.saga

import org.skull.king.application.infrastructure.framework.command.Command

interface Saga<TResponse> : Command<TResponse>
