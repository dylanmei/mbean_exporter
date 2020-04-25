package exporter

import exporter.jmx.*
import exporter.text.Vars

import java.io.Closeable

interface Writer : Closeable {
    fun write(bean: MBean)
    fun flush()
}

class StdoutWriter() : Writer {
    override fun write(bean: MBean) {
        for (attribute in bean.attributes) {
            when (attribute) {
                is Simple -> write(bean, attribute)
                is Composite -> write(bean, attribute)
            }
        }
    }

    fun write(bean: MBean, attribute: Simple) {
        val vars = Vars(bean.domain, bean.keyProperties, attribute.name)
        write(bean, vars, attribute.value)
    }

    fun write(bean: MBean, attribute: Composite) {
        attribute.items.forEach {
            val vars = Vars(bean.domain, bean.keyProperties, attribute.name + "." + it.name)
            write(bean, vars, it.value)
        }
    }

    fun write(bean: MBean, vars: Vars, value: Double) {
        val labels = bean.labels?.map {
            it.key + '=' + bean.renderLabel(it.value.name, vars)
        } ?: emptyList<String>()
        val metric = bean.renderMetric(vars) + "{${labels.joinToString(",")}}"

        println(
            "domain: ${vars.domain}, " +
            "keyprops: ${vars.keyPropString}, " +
            "attribute: ${vars.attribute}, " +
            "metric: ${metric} ${value}")
    }

    override fun flush() { }
    override fun close() { }
}
