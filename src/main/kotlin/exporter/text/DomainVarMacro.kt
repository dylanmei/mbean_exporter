package exporter.text

import com.glispa.combo.Macro

class DomainVarMacro : Macro<Vars> {
    override fun apply(str: String, vars: Vars): String {
        return str + vars.domain
    }
}
