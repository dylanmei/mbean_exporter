package exporter.text

import com.glispa.combo.Macro

class AttributeVarMacro : Macro<Vars> {
    override fun apply(str: String, vars: Vars): String {
        return str + vars.attribute
    }
}
