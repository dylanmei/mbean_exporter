package exporter.jmx

data class BeanQuery(
    val domain: String,
    val query: String,
    val attributes: Set<String>
)
