package cn.cangnova.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Serializer for Any? type
 * This serializer handles various types by converting them to appropriate JSON elements
 */
object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalArgumentException("This serializer can only be used with JSON")
        
        val jsonElement = when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                val mapContents = value.entries.associate { (k, v) ->
                    k.toString() to serializeAny(v)
                }
                JsonObject(mapContents)
            }
            is List<*> -> {
                val listContents = value.map { serializeAny(it) }
                JsonArray(listContents)
            }
            null -> JsonNull
            else -> {
                // For other types, convert to string
                JsonPrimitive(value.toString())
            }
        }
        
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalArgumentException("This serializer can only be used with JSON")
        
        return deserializeJsonElement(jsonDecoder.decodeJsonElement())
    }
    
    private fun serializeAny(value: Any?): JsonElement {
        return when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                val mapContents = value.entries.associate { (k, v) ->
                    k.toString() to serializeAny(v)
                }
                JsonObject(mapContents)
            }
            is List<*> -> {
                val listContents = value.map { serializeAny(it) }
                JsonArray(listContents)
            }
            null -> JsonNull
            else -> {
                // For other types, convert to string
                JsonPrimitive(value.toString())
            }
        }
    }
    
    private fun deserializeJsonElement(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }
            is JsonArray -> element.map { deserializeJsonElement(it) }
            is JsonObject -> element.mapValues { deserializeJsonElement(it.value) }
            JsonNull -> null
        } as Any
    }
} 