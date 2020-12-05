package exporter.config

import com.charleskorn.kaml.Yaml

class ConfigParser {
    fun parse(text: String): Config =
        Yaml.default.decodeFromString(Config.serializer(), text)
}