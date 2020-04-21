package exporter.jmx

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

import javax.management.Attribute
import javax.management.ObjectInstance
import javax.management.ObjectName

import org.slf4j.LoggerFactory

class MBeanCollector(val connection: ConnectionFactory) {
    companion object {
        val log = LoggerFactory.getLogger(MBeanCollector::class.java)
    }

    fun collect(query: MBeanQuery): List<MBeanResult> {
        val nameString = "${query.domain}:${query.query}"
        val objectName = ObjectName(nameString)

        log.debug("Querying for {}", nameString)

        val mbeans = queryObjects(objectName)
        val results = mbeans.map { mbean ->
          val attributes = queryAttributes(mbean.objectName, query.attributes)
          MBeanResult(query, mbean.objectName, attributes)
        }

        log.debug("Query found {} results for {}", results.size, nameString)

        return results
    }

    fun close() = connection.close()

    private fun queryObjects(name: ObjectName): Set<ObjectInstance> =
        connection.get().queryMBeans(name, null)

    private fun queryAttributes(name: ObjectName, attributes: Set<String>): List<Attribute> =
        connection.get().getAttributes(name, attributes.toTypedArray()).asList()
}
