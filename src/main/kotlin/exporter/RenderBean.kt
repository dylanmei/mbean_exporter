package exporter

import exporter.text.Vars

import com.glispa.combo.Template

fun BeanConfig.renderMetric(vars: Vars): String =
    template
        .render(vars)
        .replace('.', '_')

fun BeanConfig.renderLabel(name: String, vars: Vars): String =
    labels?.get(name)?.let {
        it.template.render(vars)
    } ?: ""
