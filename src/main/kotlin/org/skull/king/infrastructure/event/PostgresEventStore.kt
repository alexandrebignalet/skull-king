package org.skull.king.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.postgresql.util.PGobject
import org.skull.king.config.PostgresConfig
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.infrastructure.framework.ddd.event.Cursor
import org.skull.king.infrastructure.framework.ddd.event.Event
import org.skull.king.infrastructure.framework.ddd.event.EventStore
import org.slf4j.LoggerFactory


class PostgresEventStore(
    private val config: PostgresConfig.Connection,
    private val objectMapper: ObjectMapper
) : EventStore {

    companion object {
        private const val EVENTS_TABLE = "EVENTS"
        private const val STREAMS_TABLE = "STREAMS"

        private val LOGGER = LoggerFactory.getLogger(PostgresEventStore::class.java)
    }

    override fun <T> allOf(id: String, type: Class<T>): Cursor {
        val events = kotlin.runCatching {
            DriverManager.getConnection(config.url, config.user, config.password).use { conn ->

                val sql = """SELECT * FROM $EVENTS_TABLE WHERE stream_id = ? ORDER BY version"""

                val ps = conn.prepareStatement(sql)

                ps.setString(1, id)

                val rs = ps.executeQuery()
                val result = mutableListOf<Event>()
                while (rs.next()) {
                    result.add(objectMapper.readValue<SkullKingEvent>((rs.getObject("data") as PGobject).value as String))
                }

                result
            }
        }.getOrDefault(listOf())

        return EventCursor(events)
    }

    override fun save(events: Sequence<Event>) {
        if (events.count() == 0) return
        val aggregateId = events.first().aggregateId

        DriverManager.getConnection(config.url, config.user, config.password).use { conn ->
            conn.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            conn.autoCommit = false

            runCatching {
                createStreamIfNotExists(conn, aggregateId)

                getStream(aggregateId)?.let { stream ->
                    if (!events.all { it.version == stream.version })
                        throw OptimisticLockException(aggregateId, stream.version, events.map { it.version }.toList())
                }

                appendEvents(aggregateId, conn, events)

                incrementStreamVersion(conn, aggregateId, events.count())

                conn.commit()
                conn.autoCommit = true
            }.onFailure {
                conn.rollback()
                conn.autoCommit = true
                throw when {
                    isConcurrencyException(it) -> ConcurrentEventsException(aggregateId, it)
                    else -> it.also { LOGGER.error("Error while appending events", it) }
                }
            }
        }
    }

    private fun appendEvents(
        aggregateId: String,
        conn: Connection,
        events: Sequence<Event>
    ) {
        val sql = """INSERT INTO $EVENTS_TABLE (version, stream_id, data) VALUES (?, ?, ?::JSON)"""
        val ps = conn.prepareStatement(sql)

        var version = countEvents(aggregateId)
        events.forEach { event ->
            ps.setInt(1, version++)
            ps.setString(2, aggregateId)
            ps.setObject(3, objectMapper.writeValueAsString(event))
            ps.executeUpdate()
        }
    }

    private fun createStreamIfNotExists(conn: Connection, aggregateId: String) {
        val streamSql =
            """INSERT INTO $STREAMS_TABLE (uuid, version) VALUES (?, ?) ON CONFLICT DO NOTHING"""
        val streamPs = conn.prepareStatement(streamSql)
        streamPs.setString(1, aggregateId)
        streamPs.setInt(2, 0)
        streamPs.executeUpdate()
    }

    private fun incrementStreamVersion(conn: Connection, aggregateId: String, increment: Int) {
        val sql = """UPDATE $STREAMS_TABLE SET version = version + ? WHERE uuid = ?"""
        val ps = conn.prepareStatement(sql)
        ps.setInt(1, increment)
        ps.setString(2, aggregateId)
        ps.executeUpdate()
    }

    private fun countEvents(aggregateId: String): Int {
        DriverManager.getConnection(config.url, config.user, config.password).use { conn ->
            val sql = """SELECT COUNT(*) FROM $EVENTS_TABLE WHERE stream_id = ?"""
            val ps = conn.prepareStatement(sql)

            ps.setString(1, aggregateId)

            val rs = ps.executeQuery()
            return if (rs.next()) {
                rs.getInt(1)
            } else 0

        }
    }

    private fun getStream(id: String): Stream? =
        DriverManager.getConnection(config.url, config.user, config.password).use { conn ->
            val getStreamSql = """SELECT * FROM $STREAMS_TABLE WHERE uuid = ?"""
            val getStreamPs = conn.prepareStatement(getStreamSql)

            getStreamPs.setString(1, id)

            val rs = getStreamPs.executeQuery()
            return if (rs.next()) Stream(rs.getString("uuid"), rs.getInt("version")) else null
        }

    private fun isConcurrencyException(it: Throwable) =
        (it is SQLException && (it.sqlState == "23505" || it.sqlState == "40001")) || it is OptimisticLockException

    data class Stream(val uuid: String, val version: Int)

    class OptimisticLockException(aggregateId: String, version: Int?, wrongVersions: List<Int>) :
        RuntimeException("Aggregate $aggregateId is in version $version not in $wrongVersions")
}
