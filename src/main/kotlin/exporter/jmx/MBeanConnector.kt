package exporter.jmx

import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

import javax.management.AttributeList
import javax.management.MBeanInfo
import javax.management.ObjectInstance
import javax.management.ObjectName

import java.io.IOException

class MBeanConnector(
    host: String,
    port: Int,
    username: String? = null,
    password: String? = null
) : AutoCloseable {
    val environment = HashMap<String, Any>()
    val connector: JMXConnector

    init {
        username?.let {
            environment[JMXConnector.CREDENTIALS] = arrayOf(username, password)
        }

        connector = open(host, port)
    }

    fun open(host: String, port: Int): JMXConnector = try {
        val url = JMXServiceURL("service:jmx:rmi:///jndi/rmi://${host}:${port}/jmxrmi")
        JMXConnectorFactory.connect(url, environment)
    } catch (ioe: IOException) {
        when (ioe.cause) {
            is javax.naming.ServiceUnavailableException ->
                throw MBeanConnectorException("Could not connect to JMX service at ${host}:${port}", ioe.cause)
            else ->
                throw MBeanConnectorException("Could not open JMX connector: ${ioe.localizedMessage}", ioe)
        }
    }

    fun queryMBeans(name: ObjectName): MutableSet<ObjectInstance> =
        connector.mBeanServerConnection.queryMBeans(name, null)

    fun getMBeanInfo(name: ObjectName): MBeanInfo =
        connector.mBeanServerConnection.getMBeanInfo(name)

    fun getAttributes(name: ObjectName, attributeNames: List<String>): AttributeList =
        connector.mBeanServerConnection.getAttributes(name, attributeNames.toTypedArray())

    override fun close() {
        connector.close()
    }
}
