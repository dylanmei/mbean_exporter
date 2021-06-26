package exporter.text

import com.glispa.combo.Macro

class LowerCaseMacro() : Macro<Vars> {
    override fun apply(str: String, vars: Vars): String {
        return str.lowercase()
    }
}
