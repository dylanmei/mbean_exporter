package exporter.jmx

import java.util.*
import javax.management.MBeanServerConnection
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import javax.rmi.ssl.SslRMIClientSocketFactory

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

        connector = JMXConnectorFactory.connect(
            JMXServiceURL("service:jmx:rmi:///jndi/rmi://${host}:${port}/jmxrmi"), environment)
    }

    fun conn(): MBeanServerConnection {
        return connector.mBeanServerConnection
    }

    override fun close() {
        connector.close()
    }
}
