package exporter.jmx

import javax.management.Attribute
import javax.management.ObjectName

data class MBeanResult(
    val query: MBeanQuery,
    val objectName: ObjectName,
    val attributes: List<Attribute>
)
