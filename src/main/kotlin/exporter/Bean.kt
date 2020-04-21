package exporter

import exporter.jmx.MBeanResult
import exporter.text.Vars

import com.glispa.combo.Template

data class Bean(
    val domain: String,
    val config: BeanConfig,
    val keyProperties: Map<String, String>,
    val attributes: Map<String, Double>
) {
    val query: String
        get() = config.query

    fun renderMetric(vars: Vars): String =
        config.template
            .render(vars)
            .replace('.', '_')

    fun renderLabel(name: String, vars: Vars): String {
        return config.labels?.get(name)?.let {
                it.template.render(vars)
            } ?: ""
    }
}

fun MBeanResult.sample(): Bean {
    val attrs = this.attributes.map {
        val value = it.value as Number? ?: 0
        it.name to value.toDouble()
    }

    return Bean(
        this.query.domain,
        this.query.context as BeanConfig,
        this.objectName.getKeyPropertyList().toMap(),
        attrs.toMap())
}
