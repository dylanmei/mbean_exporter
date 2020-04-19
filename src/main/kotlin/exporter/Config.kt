package exporter

import exporter.text.*

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.MalformedYamlException

import com.glispa.combo.Template

import kotlinx.serialization.*
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.serializer

import picocli.CommandLine
import java.io.File

@Serializable
data class Config(
    val domains: List<DomainConfig>
)

@Serializable
data class DomainConfig(
    val name: String,
    val beans: List<BeanConfig>
)

@Serializable
data class BeanConfig(
    val query: String,
    val attributes: AttributesConfig,
    val metric: String? = null,
    val labels: LabelsConfig? = null
) {
    @Transient
    var template: Template<Vars> = metric?.let {
            Template(VarMacroRegistry, metric)
        } ?: Template(VarMacroRegistry, "bean")
}

@Serializable
class AttributesConfig(
    val attributes: Set<AttributeConfig>
) : HashSet<AttributeConfig>(attributes) {
    val names: Set<String>
        get() = attributes.map { it.name }.toSet()

    @Serializer(forClass=AttributesConfig::class)
    companion object : KSerializer<AttributesConfig> {
        val innerSerializer = SetSerializer(MapEntrySerializer<String, String>(String.serializer(), String.serializer()))

        @ImplicitReflectionSerializer
        override val descriptor: SerialDescriptor
            get() = SerialDescriptor("AttributesConfig") {
                element<Map.Entry<String, String>>("attributes")
            }

        override fun serialize(encoder: Encoder, value: AttributesConfig) = throw UnsupportedOperationException()

        @ImplicitReflectionSerializer
        override fun deserialize(decoder: Decoder): AttributesConfig {
            val encodedSet = innerSerializer.deserialize(decoder)
            val deserializedSet = encodedSet.map {
                AttributeConfig(it.key, AttributeType.valueOf(it.value.toUpperCase()))
            }.toSet()

            return AttributesConfig(deserializedSet)
        }
    }

}

@Serializable
data class AttributeConfig(val name: String, val type: AttributeType)

@Serializable
enum class AttributeType {
    @SerialName("gauge") GAUGE,
    @SerialName("counter") COUNTER;
}

@Serializable
class LabelsConfig(
    val labels: Map<String, LabelConfig>
) : HashMap<String, LabelConfig>(labels) {

    @Serializer(forClass=LabelsConfig::class)
    companion object : KSerializer<LabelsConfig> {
        val innerSerializer = MapSerializer(String.serializer(), String.serializer())

        @ImplicitReflectionSerializer
        override val descriptor: SerialDescriptor
            get() = SerialDescriptor("LabelsConfig") {
                element<Map<String, String>>("labels")
            }

        override fun serialize(encoder: Encoder, value: LabelsConfig) = throw UnsupportedOperationException()

        override fun deserialize(decoder: Decoder): LabelsConfig {
            val encodedMap = innerSerializer.deserialize(decoder)
            val deserializedMap = encodedMap.map {
                it.key to LabelConfig(it.key, it.value)
            }.toMap()
            return LabelsConfig(deserializedMap)
        }
    }
}

@Serializable
data class LabelConfig(val name: String, val text: String) {
    @Transient
    val template: Template<Vars> = Template(VarMacroRegistry, text)
}

class ConfigConverter: CommandLine.ITypeConverter<Config> {
    @Throws(MalformedYamlException::class)
    override fun convert(configPath: String): Config {
        val text = File(configPath).readText(Charsets.UTF_8)
        return ConfigParser().parse(text)
    }
}

class ConfigParser {
    fun parse(text: String): Config =
        Yaml.default.parse(Config.serializer(), text)
}
