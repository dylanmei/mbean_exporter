package exporter.jmx

import org.slf4j.LoggerFactory
import java.io.IOException
import javax.management.*
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

class MBeanConnector(
    val host: String,
    val port: Int,
    username: String? = null,
    password: String? = null
) : AutoCloseable, NotificationListener {
    val environment = HashMap<String, Any>()
    var connector: JMXConnector
    var connected: Boolean

    init {
        username?.let {
            environment[JMXConnector.CREDENTIALS] = arrayOf(username, password)
        }

        log.debug("Connecting to JMX service at $host:$port")

        connected = false
        connector = connect()
    }

    private fun connect(): JMXConnector = try {
        val conn = JMXConnectorFactory.newJMXConnector(
            JMXServiceURL("service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi"),
            environment
        )
        conn.addConnectionNotificationListener(this, null, null)
        conn.connect()
        conn
    } catch (ioe: IOException) {
        when (ioe.cause) {
            is javax.naming.ServiceUnavailableException ->
                throw MBeanConnectorException(CONNECT_ERR_TEMPLATE.format(host, port), ioe.cause)
            else ->
                throw MBeanConnectorException(
                    "Could not open JMX connector: ${ioe.localizedMessage}",
                    ioe
                )
        }
    }

    private fun reconnect() {
        log.debug("Reconnecting to JMX service at $host:$port")
        connector = connect()
    }

    override fun handleNotification(notification: Notification, _handback: Any?) {
        when (notification.type) {
            NOTIFICATION_OPENED -> {
                connected = true
                log.debug("JMX connection to $host:$port is open")
            }
            NOTIFICATION_CLOSED -> {
                connected = false
                connector.removeConnectionNotificationListener(this)
                log.debug("JMX connection to $host:$port is closed")
            }
        }
    }

    fun queryMBeans(name: ObjectName): MutableSet<ObjectInstance> = try {
        if (!connected) reconnect()

        connector.mBeanServerConnection.queryMBeans(name, null)
    } catch (ioe: IOException) {
        throw MBeanConnectorException(DISCONNECT_ERR_TEMPLATE.format(host, port), ioe)
    }

    fun getMBeanInfo(name: ObjectName): MBeanInfo = try {
        if (!connected) reconnect()

        connector.mBeanServerConnection.getMBeanInfo(name)
    } catch (ioe: IOException) {
        throw MBeanConnectorException(DISCONNECT_ERR_TEMPLATE.format(host, port), ioe)
    }

    fun getAttributes(name: ObjectName, attributeNames: List<String>): AttributeList = try {
        if (!connected) reconnect()

        connector.mBeanServerConnection.getAttributes(name, attributeNames.toTypedArray())
    } catch (ioe: IOException) {
        throw MBeanConnectorException(DISCONNECT_ERR_TEMPLATE.format(host, port), ioe)
    }

    override fun close() {
        connector.removeConnectionNotificationListener(this)
        connector.close()
    }

    companion object {
        val log = LoggerFactory.getLogger(MBeanConnector::class.java)!!
        const val NOTIFICATION_OPENED = "jmx.remote.connection.opened"
        const val NOTIFICATION_CLOSED = "jmx.remote.connection.closed"
        const val CONNECT_ERR_TEMPLATE = "Could not connect to JMX service at %s:%d"
        const val DISCONNECT_ERR_TEMPLATE = "Could not communicate with JMX service at %s:%d"
    }
}
