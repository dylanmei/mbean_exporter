package exporter.text

import com.glispa.combo.Macro

class ReplaceMacro(val args: Array<String>) : Macro<Vars> {
    val regex = Regex(args.first())
    val substitution = if (args.size == 1) "" else args[1]

    override fun apply(str: String, vars: Vars): String {
        return str.replace(regex, substitution)
    }
}

