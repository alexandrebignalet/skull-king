package org.skull.king.resource.healthcheck

import com.codahale.metrics.health.HealthCheck


class BaseHealthCheck : HealthCheck() {

    override fun check(): Result = Result.healthy()
}
