package exporter.jmx

import javax.management.Attribute
import javax.management.ObjectName

data class MBean(
    val query: MBeanQuery,
    val keyProperties: Map<String, String>,
    val attributes: List<MBeanAttribute>
) {
    val domain: String
        get() = query.domain
}
