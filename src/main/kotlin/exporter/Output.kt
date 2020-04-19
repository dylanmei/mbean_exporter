package exporter

import picocli.CommandLine

enum class OutputOption(val option: String) {
    HTTP("http"),
    STDOUT("stdout");
}

class OutputOptionConverter: CommandLine.ITypeConverter<OutputOption> {
    @Throws(Exception::class)
    override fun convert(option: String): OutputOption {
        val selectedOption = OutputOption.values().find {
            it.option.toLowerCase() == option
        }

        if (selectedOption != null) {
            return selectedOption
        }

        throw java.lang.Exception("Could not recognize '$option' output option")
    }
}
