package exporter.text

import com.glispa.combo.Macro

class KeyPropsVarMacro() : Macro<Vars> {
    override fun apply(str: String, vars: Vars): String {
        return str + vars.keyPropString
    }
}
