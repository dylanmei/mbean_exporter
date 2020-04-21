package exporter.jmx

data class MBeanQuery(
    val context: Any,
    val domain: String,
    val query: String,
    val attributes: Set<String>
)
