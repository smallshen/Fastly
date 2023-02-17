package org.endoqa.fastly

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.connection.PlayerConnection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.GameProfile
import org.endoqa.fastly.player.Property
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.EncryptionResponsePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginPluginResponsePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import org.endoqa.fastly.protocol.packet.server.login.EncryptionRequestPacket
import org.endoqa.fastly.protocol.packet.server.login.LoginPluginRequestPacket
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun FastlyServer.handleOnlineLogin(connection: Connection, handshakePacket: HandshakePacket): PlayerConnection {
    val rp = connection.readRawPacket()
    require(rp.packetId == 0x00) { "Expected login packet, got ${rp.packetId}" }

    val loginStartPacket = LoginStartPacket.read(ByteBuf(rp.buffer.position(0)))
    if (!loginStartPacket.hasPlayerUUID || loginStartPacket.playerUUID == null) {
        error("Player UUID is null")
    }

    val nonce = ByteArray(4)
    ThreadLocalRandom.current().nextBytes(nonce)
    val encryptionRequestPacket = EncryptionRequestPacket("", this.keyPair.public.encoded, nonce)
    connection.sendPacket(encryptionRequestPacket) // maybe we don't need to join. we'll se in the future

    val ep = connection.readRawPacket()
    require(ep.packetId == 0x01) { "Expected encryption response packet, got ${ep.packetId}" }

    val encryptionResponsePacket = EncryptionResponsePacket.read(ByteBuf(ep.buffer.position(0)))

    val verifyCipher = Cipher.getInstance("RSA")
    verifyCipher.init(Cipher.DECRYPT_MODE, this.keyPair.private)

    val checkVerifyToken = verifyCipher.doFinal(encryptionResponsePacket.verifyToken).contentEquals(nonce)
    require(checkVerifyToken) { "Verify token is not correct" }

    val secretCipher = Cipher.getInstance("RSA")
    secretCipher.init(Cipher.DECRYPT_MODE, this.keyPair.private)
    val secret = secretCipher.doFinal(encryptionResponsePacket.sharedSecret) ?: error("Shared secret is null")

    val secretKey = SecretKeySpec(secret, "AES")

    val digestedData = MessageDigest.getInstance("SHA-1")
    digestedData.update(secretKey.encoded)
    digestedData.update(keyPair.public.encoded)

    val serverIdBytes = digestedData.digest()
    val serverId = BigInteger(serverIdBytes).toString(16)
    val profile = hasJoined(loginStartPacket.name, serverId)

    connection.enableEncryption(secretKey)

    require(profile.uuid == loginStartPacket.playerUUID) { "UUID is not correct" }

    val playerCon = PlayerConnection(connection)
    playerCon.connectToBackend(backendServers.first())

    val backend = playerCon.backendConnection
    backend.startIO()

    backend.sendPacket(handshakePacket)
    backend.sendPacket(loginStartPacket)


    val loginRequestRp = backend.readRawPacket()
    require(loginRequestRp.packetId == LoginPluginRequestPacket.packetId) { "Expected login plugin response packet, got ${loginRequestRp.packetId}" }

    val loginRequest = LoginPluginRequestPacket.read(ByteBuf(loginRequestRp.buffer.position(0)))

    val loginResponsePacket = LoginPluginResponsePacket(
        loginRequest.messageId,
        true,
        createForwardingData(connection.remoteAddressAsString(), profile)
    )


    backend.sendPacket(loginResponsePacket)

    return playerCon
}

private fun FastlyServer.createForwardingData(
    actualAddress: String,
    gameProfile: GameProfile
): ByteArray {
    val buffer = ByteBuffer.allocate(2048) //TODO: replace 2048 with estimatedSize
    val buf = ByteBuf(buffer)
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

    buffer.flip()

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(modernForwardKey)
    mac.update(buffer.array(), 0, buffer.limit())
    val sig = mac.doFinal()

    val arr = ByteArray(buffer.remaining())
    buffer.get(arr)

    return sig + arr
}


private fun Connection.remoteAddressAsString(): String {
    val addr = (socket.channel.remoteAddress as InetSocketAddress).address.hostAddress

    val ipv6ScopeIdx = addr.indexOf('%')
    return if (ipv6ScopeIdx == -1) {
        addr
    } else {
        addr.substring(0, ipv6ScopeIdx)
    }
}

private val json = Json {
    serializersModule = SerializersModule {
        contextual(GameProfile.serializer())
        contextual(Property.serializer())
    }
}


private suspend fun hasJoined(
    name: String,
    serverId: String
) = suspendCancellableCoroutine<GameProfile> { continuation ->
    val username = URLEncoder.encode(name, StandardCharsets.UTF_8)
    val url =
        "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${username}&serverId=${serverId}"

    val client = HttpClient.newHttpClient()
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    val future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())

    continuation.invokeOnCancellation {
        future.cancel(true)
    }

    future.whenComplete { response, throwable ->
        if (throwable != null) {
            continuation.resumeWithException(throwable)
            return@whenComplete
        }

        if (response.statusCode() != 200) {
            continuation.resumeWithException(IllegalStateException("Invalid status code: ${response.statusCode()}"))
            return@whenComplete
        }

        try {
            val data: GameProfile = json.decodeFromString(response.body())
            continuation.resume(data)
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
            return@whenComplete
        }
    }


}