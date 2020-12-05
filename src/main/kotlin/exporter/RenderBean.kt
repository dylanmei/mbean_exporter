package exporter

import exporter.config.BeanConfig
import exporter.text.Vars

fun BeanConfig.renderMetric(vars: Vars): String =
    template
        .render(vars)
        .replace('.', '_')

fun BeanConfig.renderLabel(name: String, vars: Vars): String =
    labels?.get(name)?.let {
        it.template.render(vars)
    } ?: ""
