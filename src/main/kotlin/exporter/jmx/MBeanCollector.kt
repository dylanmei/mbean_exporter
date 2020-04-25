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
        val results = mbeans
            .map { mbean -> collectObject(query, mbean.objectName, query.attributes) }
            .filter { it.attributes.size > 0 }

        log.debug("Query found {} results for {}", results.size, nameString)
        return results
    }

    fun close() = connection.close()

    private fun collectObject(query: MBeanQuery, objectName: ObjectName, attributeNames: Set<String>): MBeanResult {
        log.debug("Query attributes for {}", objectName)

        val attributes = queryAttributes(objectName, attributeNames)
        return MBeanResult(query, objectName, attributes)
    }

    private fun queryObjects(name: ObjectName): Set<ObjectInstance> =
        connection.get().queryMBeans(name, null)

    private fun queryAttributes(objectName: ObjectName, attributeNames: Set<String>): List<Attribute> {
        val conn = connection.get()
        val availableNames = conn
            .getMBeanInfo(objectName)
            .getAttributes()
            .filter { it.isReadable() && attributeNames.contains(it.name) }
            .map { it.name }

        if (availableNames.isEmpty()) {
            return emptyList()
        }

        return conn.getAttributes(objectName, availableNames.toTypedArray()).asList()
    }
}
