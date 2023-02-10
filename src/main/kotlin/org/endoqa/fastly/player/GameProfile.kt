package org.endoqa.fastly.player


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*


private val uuidRegex = "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex()

@Serializable
data class GameProfile(
    val id: String,
    val name: String,
    val properties: List<Property>
) {
    @Transient
    val uuid = UUID.fromString(id.replaceFirst(uuidRegex, "$1-$2-$3-$4-$5"))!!
}

@Serializable
data class Property(
    val name: String,
    val value: String,
    val signature: String
)