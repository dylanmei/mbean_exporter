package exporter.text

import com.glispa.combo.Macro

class UpperCaseMacro() : Macro<Vars> {
    override fun apply(str: String, vars: Vars): String {
        return str.toUpperCase()
    }
}
