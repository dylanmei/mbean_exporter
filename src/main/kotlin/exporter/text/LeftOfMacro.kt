package exporter.text

import com.glispa.combo.Macro

class LeftOfMacro(val pattern: String) : Macro<Vars> {
    val regex = when (pattern) {
        "" -> Regex("\\s+")
        "." -> Regex("\\.+")
        else -> Regex(pattern)
    }

    override fun apply(str: String, vars: Vars): String {
        return str.split(regex).first()
    }
}
