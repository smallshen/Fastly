package org.endoqa.fastly

import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.GrowingByteBuf
import org.endoqa.fastly.player.GameProfile
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import javax.crypto.Mac

fun FastlyServer.createForwardingData(
    actualAddress: String,
    gameProfile: GameProfile
): ByteArray {
    val buf = GrowingByteBuf(2048)
    buf.writeVarInt(1) // MODERN_FORWARDING_DEFAULT, no support for chat sign
    buf.writeString(actualAddress)
    buf.writeUUID(gameProfile.uuid)
    buf.writeString(gameProfile.name)

    buf.writeVarInt(gameProfile.properties.size)
    gameProfile.properties.forEach { property ->
        buf.writeString(property.name)
        buf.writeString(property.value)
        buf.writeBoolean(property.signature != null)
        property.signature?.let { buf.writeString(it) }
    }

    val buffer = buf.buf

    buffer.flip()

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(modernForwardKey)
    mac.update(buffer.array(), 0, buffer.limit())
    val sig = mac.doFinal()

    val arr = ByteArray(buffer.remaining())
    buffer.get(arr)

    return sig + arr
}


fun Connection.remoteAddressAsString(): String {
    val addr = (socket.channel.remoteAddress as InetSocketAddress).address.hostAddress

    val ipv6ScopeIdx = addr.indexOf('%')
    return if (ipv6ScopeIdx == -1) {
        addr
    } else {
        addr.substring(0, ipv6ScopeIdx)
    }
}