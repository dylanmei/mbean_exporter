package exporter.config

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AttributesConfigSerializer : KSerializer<AttributesConfig> {
    val innerSerializer = SetSerializer(MapEntrySerializer(String.serializer(), String.serializer()))

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("AttributesConfig", SerialKind.CONTEXTUAL) {
            element<Map.Entry<String, String>>("attributes")
        }

    override fun serialize(encoder: Encoder, value: AttributesConfig) = throw UnsupportedOperationException()

    override fun deserialize(decoder: Decoder): AttributesConfig {
        val configs = HashMap<String, MutableList<AttributeConfig>>()
        innerSerializer.deserialize(decoder)
            .forEach { (name, type) ->
                val segments = name.split(".", limit = 2)
                if (segments.size == 1) {
                    configs[name] = mutableListOf(AttributeConfig(name, AttributeType.valueOf(type.uppercase())))
                } else {
                    val (attrName, itemName) = segments

                    var list = configs[attrName]
                    if (list == null) {
                        list = mutableListOf()
                        configs[attrName] = list
                    }

                    list.add(AttributeConfig(itemName, AttributeType.valueOf(type.uppercase())))
                }
            }

        return AttributesConfig(
            configs.map { (name, items) ->
                if (items.size == 1) {
                    AttributeConfig(name, items.first().type)
                } else {
                    AttributeConfig(name, AttributeType.COMPOSITE, items.toSet())
                }
            }.toSet())
    }
}

object LabelsConfigSerializer : KSerializer<LabelsConfig> {
    val innerSerializer = MapSerializer(String.serializer(), String.serializer())

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("LabelsConfig", SerialKind.CONTEXTUAL) {
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
