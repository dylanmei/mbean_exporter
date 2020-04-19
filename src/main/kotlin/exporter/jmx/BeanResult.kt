package exporter.jmx

import javax.management.Attribute
import javax.management.ObjectName

data class BeanResult(
    val query: BeanQuery,
    val objectName: ObjectName,
    val attributes: List<Attribute>
)
