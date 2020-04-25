package exporter.text

import com.glispa.combo.MacroRegistry

object VarMacroRegistry: MacroRegistry<Vars>() {
    init {
        register("domain", { _ -> DomainVarMacro() })
        register("keyprop", { args -> KeyPropVarMacro(args.first()) })
        register("keyprops", { _ -> KeyPropsVarMacro() })
        register("attribute", { _ -> AttributeVarMacro() })
        register("lower", { _ -> LowerCaseMacro() })
        register("upper", { _ -> UpperCaseMacro() })
        register("snake", { _ -> SnakeCaseMacro() })
        register("replace", { args -> ReplaceMacro(args) })
        register("leftof", { args -> LeftOfMacro(args.firstOrNull() ?: "") })
        register("rightof", { args -> RightOfMacro(args.firstOrNull() ?: "") })
    }
}

