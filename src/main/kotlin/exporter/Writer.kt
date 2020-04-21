package exporter

import exporter.text.*
import java.io.Closeable

interface Writer : Closeable {
    fun write(bean: Bean)
    fun flush()
}

class StdoutWriter() : Writer {
    override fun write(bean: Bean) {
        val beanConfig = bean.config
        bean.attributes.forEach {
            val vars = Vars(bean.domain, bean.keyProperties, it.key)
            val labels = beanConfig.labels?.map {
                it.key + '=' + bean.renderLabel(it.value.name, vars)
            } ?: emptyList<String>()

            println(
                "Domain: ${vars.domain}, " +
                "KeyProperties: ${vars.keyPropString}, " +
                "Attribute: ${it.key}=${it.value}, " +
                "Metric: ${bean.renderMetric(vars)}, " +
                "Labels: ${labels}")
        }
    }

    override fun flush() { }
    override fun close() { }
}
