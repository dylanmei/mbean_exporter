package exporter.config

import com.glispa.combo.Template
import exporter.text.VarMacroRegistry
import exporter.text.Vars
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val pattern: String,
    val attributes: AttributesConfig,
    val metric: String? = null,
    val labels: LabelsConfig? = null
) {
    @Transient
    var template: Template<Vars> = metric?.let {
        Template(VarMacroRegistry, metric)
    } ?: Template(VarMacroRegistry, "mbean")
}

@Serializable(with = AttributesConfigSerializer::class)
class AttributesConfig(
    val attributes: Set<AttributeConfig>
) : HashSet<AttributeConfig>(attributes)

@Serializable
data class AttributeConfig(
    val name: String,
    val type: AttributeType,
    val items: Set<AttributeConfig> = emptySet()
)

@Serializable
enum class AttributeType {
    @SerialName("unknown")
    UNKNOWN,

    @SerialName("gauge")
    GAUGE,

    @SerialName("counter")
    COUNTER,
    COMPOSITE;
}

@Serializable(with = LabelsConfigSerializer::class)
class LabelsConfig(
    val labels: Map<String, LabelConfig>
) : HashMap<String, LabelConfig>(labels)

@Serializable
data class LabelConfig(val name: String, val text: String) {
    @Transient
    val template: Template<Vars> = Template(VarMacroRegistry, text)
}
