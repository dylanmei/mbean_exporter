package exporter.jmx

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

import javax.management.Attribute
import javax.management.ObjectInstance
import javax.management.ObjectName

import org.slf4j.LoggerFactory

class Collector(val connection: ConnectionFactory) {
    data class Query(
        val domain: String,
        val query: String,
        val attributes: Set<String>
    )

    data class Result(
        val context: Any,
        val query: Query,
        val objectName: ObjectName,
        val attributes: List<Attribute>
    )

    companion object {
        val log = LoggerFactory.getLogger(Collector::class.java)
        const val channelBuffer: Int = 1024
    }

    val exceptionHandler = CoroutineExceptionHandler { _, ex ->
        log.error("Could not query beans: {}", ex.message)
    }

    suspend fun query(context: Any, query: Query): ReceiveChannel<Result> = coroutineScope jmxQuery@{
        var channel = Channel<Result>(channelBuffer)
        val name = "${query.domain}:${query.query}"
        val objectName = ObjectName(name)

        log.debug("Querying for {}", name)

        launch(exceptionHandler) {
            queryObjects(objectName).map { mbean ->
                async(exceptionHandler) {
                    log.debug("{} result: {}", name, mbean)

                    val attributes = queryAttributes(mbean.objectName, query.attributes)
                    channel.send(Result(context, query, mbean.objectName, attributes))
                }
            }.awaitAll()

            channel.close()
        }

        return@jmxQuery channel
    }

    fun close() = connection.close()

    private fun queryObjects(name: ObjectName): Set<ObjectInstance> =
        connection.get().queryMBeans(name, null)

    private fun queryAttributes(name: ObjectName, attributes: Set<String>): List<Attribute> =
        connection.get().getAttributes(name, attributes.toTypedArray()).asList()
}
