package cash.atto.signature

import cash.atto.commons.AttoAddress
import cash.atto.commons.AttoAlgorithm
import cash.atto.commons.AttoBlock
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoVote
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LegacyBlockInputSerializer : JsonTransformingSerializer<AttoBlock>(AttoBlock.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val block = element.jsonObject
        return block.withCanonicalAddresses((block["type"] as? JsonPrimitive)?.content)
    }
}

object LegacyVoteInputSerializer : JsonTransformingSerializer<AttoVote>(AttoVote.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement = element.jsonObject.withCanonicalAddresses()
}

private fun JsonObject.withCanonicalAddresses(blockType: String? = null): JsonObject {
    val fields = toMutableMap()
    val identities = mutableListOf("address" to "")
    when (blockType) {
        "OPEN", "CHANGE" -> identities.add("representativeAddress" to "representative")
        "SEND" -> identities.add("receiverAddress" to "receiver")
    }
    for ((addressName, prefix) in identities) {
        val algorithmName = if (prefix.isEmpty()) "algorithm" else "${prefix}Algorithm"
        val keyName = if (prefix.isEmpty()) "publicKey" else "${prefix}PublicKey"
        val algorithm = fields.remove(algorithmName)
        val publicKey = fields.remove(keyName)
        if (!fields.containsKey(addressName) && (algorithm != null || publicKey != null)) {
            if (algorithm == null || publicKey == null) {
                throw SerializationException("$addressName requires both $algorithmName and $keyName")
            }
            fields[addressName] =
                try {
                    JsonPrimitive(
                        AttoAddress(
                            AttoAlgorithm.valueOf(algorithm.jsonPrimitive.content),
                            AttoPublicKey.parse(publicKey.jsonPrimitive.content),
                        ).toString(),
                    )
                } catch (exception: IllegalArgumentException) {
                    throw SerializationException("Invalid legacy identity for $addressName", exception)
                }
        }
    }
    return JsonObject(fields)
}
