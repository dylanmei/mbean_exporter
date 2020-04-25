package exporter.jmx

sealed class MBeanAttribute
data class Simple(val name: String, val value: Double) : MBeanAttribute()
data class Composite(val name: String, val items: Collection<Simple>) : MBeanAttribute()
