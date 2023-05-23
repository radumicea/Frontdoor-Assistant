package iiotca.frontdoorassistant.data.dto

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.Base64

data class HistoryItem(val name: String, val timeStamp: Long, val data: ByteArray) {
    class Deserializer : ResponseDeserializable<List<HistoryItem>> {
        override fun deserialize(content: String): List<HistoryItem> {
            val builder = GsonBuilder()
            builder.registerTypeAdapter(
                HistoryItem::class.java,
                object : JsonDeserializer<HistoryItem> {
                    override fun deserialize(
                        json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
                    ): HistoryItem {
                        val obj = json.asJsonObject

                        return HistoryItem(
                            obj["name"].asString,
                            obj["timeStamp"].asLong,
                            Base64.getDecoder().decode(obj["data"].asString)
                        )
                    }
                })

            return builder.create()
                .fromJson(content, object : TypeToken<List<HistoryItem>>() {}.type)
        }
    }
}