package exporter.jmx

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

import javax.management.Attribute
import javax.management.MBeanAttributeInfo
import javax.management.ObjectInstance
import javax.management.ObjectName
import javax.management.openmbean.CompositeType
import javax.management.openmbean.CompositeData

import org.slf4j.LoggerFactory

class MBeanCollector(val connector: MBeanConnector) {
    companion object {
        val log = LoggerFactory.getLogger(MBeanCollector::class.java)
    }

    fun collect(query: MBeanQuery): List<MBean> {
        val nameString = "${query.domain}:${query.query}"
        val objectName = ObjectName(nameString)

        log.debug("Collecting beans for {}", nameString)

        val mbeans = queryObjects(objectName)
        val results = mbeans
            .map { mbean -> collectObject(query, mbean.objectName) }
            .filter { it.attributes.size > 0 }

        log.debug("Collected {} beans for {}", results.size, nameString)
        return results
    }

    fun close() = connector.close()

    private fun collectObject(query: MBeanQuery, objectName: ObjectName): MBean {
        return MBean(
            query,
            objectName.keyPropertyList.toMap(),
            collectAttributes(objectName, query.attributes))
    }

    private fun collectAttributes(objectName: ObjectName, attributeNames: Set<String>): List<MBeanAttribute> {
        log.debug("Collecting attributes for {}", objectName)

        val attributes = mutableListOf<MBeanAttribute>()
        queryAttributes(objectName, attributeNames)
            .forEach { attribute ->
                val valueObject = attribute.value
                when (valueObject) {
                    is Number -> {
                        attributes.add(Simple(attribute.name, valueObject.toDouble()))
                    }
                    is CompositeData -> {
                        val items = valueObject.compositeType
                            .keySet()
                            .mapNotNull { itemName ->
                                val scalar = valueObject.get(itemName) as Number?
                                scalar?.run {
                                    Simple(itemName, toDouble())
                                }
                            }

                        attributes.add(Composite(attribute.name, items))
                    }
                    else -> log.debug("Dropping strange attribute {}", attribute.name)
                }
            }

        return attributes
    }

    private fun queryObjects(name: ObjectName): Set<ObjectInstance> =
        connector.conn().queryMBeans(name, null)

    private fun queryAttributes(objectName: ObjectName, names: Set<String>): List<Attribute> {
        val conn = connector.conn()
        val availableNames = conn
            .getMBeanInfo(objectName)
            .getAttributes()
            .filter { it.isReadable() && names.contains(it.name) }
            .map { it.name }

        if (availableNames.isEmpty()) return emptyList()
        return conn
            .getAttributes(objectName, availableNames.toTypedArray())
            .asList()
    }
}
