package exporter

import exporter.jmx.MBean
import exporter.text.Vars

import com.glispa.combo.Template

val MBean.config
    get() = this.query.context as BeanConfig

val MBean.template
    get() = (this.query.context as BeanConfig).template

val MBean.labels
    get() = (this.query.context as BeanConfig).labels

fun MBean.renderMetric(vars: Vars): String =
    template
        .render(vars)
        .replace('.', '_')

fun MBean.renderLabel(name: String, vars: Vars): String =
    labels?.get(name)?.let {
        it.template.render(vars)
    } ?: ""

