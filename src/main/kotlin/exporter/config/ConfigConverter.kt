package exporter.config

import com.charleskorn.kaml.MalformedYamlException
import picocli.CommandLine
import java.io.File

class ConfigConverter: CommandLine.ITypeConverter<Config> {
    @Throws(MalformedYamlException::class)
    override fun convert(configPath: String): Config {
        val text = File(configPath).readText(Charsets.UTF_8)
        return ConfigParser().parse(text)
    }
}