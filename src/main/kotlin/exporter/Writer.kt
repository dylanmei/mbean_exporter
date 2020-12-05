package exporter

import exporter.config.*
import exporter.text.Vars

import java.io.Closeable

interface Writer : Closeable {
    fun write(beanConfig: BeanConfig, vars: Vars, type: AttributeType, value: Double)
    fun flush()
}

class StdoutWriter() : Writer {
    override fun write(beanConfig: BeanConfig, vars: Vars, type: AttributeType, value: Double) {
        val prefixString = "domain: ${vars.domain}, keyprops: ${vars.keyPropString}, attribute: ${vars.attribute}, ${type}: "
        val metricString = beanConfig.renderMetric(vars)
        val labelsString = beanConfig.labels?.map {
            it.key + '=' + beanConfig.renderLabel(it.value.name, vars)
        }?.joinToString(",") ?: ""

        println("${prefixString}${metricString}{${labelsString}} ${value}")
    }

    override fun flush() { }
    override fun close() { }
}
