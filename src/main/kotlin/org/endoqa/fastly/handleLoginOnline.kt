package org.endoqa.fastly

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.player.GameProfile
import org.endoqa.fastly.player.Property
import org.endoqa.fastly.protocol.packet.client.handshake.HandshakePacket
import org.endoqa.fastly.protocol.packet.client.login.EncryptionResponsePacket
import org.endoqa.fastly.protocol.packet.client.login.LoginStartPacket
import org.endoqa.fastly.protocol.packet.server.login.EncryptionRequestPacket
import java.math.BigInteger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun FastlyServer.handleOnlineLogin(connection: Connection, handshakePacket: HandshakePacket) {
    val rp = connection.packetIn.receive()
    require(rp.packetId == 0x00) { "Expected login packet, got ${rp.packetId}" }

    val loginStartPacket = LoginStartPacket.read(ByteBuf(rp.buffer.position(0)))
    if (!loginStartPacket.hasPlayerUUID || loginStartPacket.playerUUID == null) {
        error("Player UUID is null")
    }

    val nonce = ByteArray(4)
    ThreadLocalRandom.current().nextBytes(nonce)
    val encryptionRequestPacket = EncryptionRequestPacket("", this.keyPair.public.encoded, nonce)
    connection.sendPacket(encryptionRequestPacket) // maybe we don't need to join. we'll se in the future

    val ep = connection.packetIn.receive()
    require(ep.packetId == 0x01) { "Expected encryption response packet, got ${ep.packetId}" }

    val encryptionResponsePacket = EncryptionResponsePacket.read(ByteBuf(ep.buffer.position(0)))

    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.DECRYPT_MODE, this.keyPair.private)

    require(cipher.doFinal(encryptionResponsePacket.verifyToken).contentEquals(nonce)) { "Verify token is not correct" }

    val cipher4Secret = Cipher.getInstance("RSA")
    cipher4Secret.init(Cipher.DECRYPT_MODE, this.keyPair.private)

    val secret = cipher4Secret.doFinal(encryptionResponsePacket.sharedSecret) ?: error("Shared secret is null")

    val secretKey = SecretKeySpec(secret, "AES")

    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(secretKey.encoded)
    digest.update(keyPair.public.encoded)

    val serverIdBytes = digest.digest()
    val serverId = BigInteger(serverIdBytes).toString(16)
    val profile = hasJoined(loginStartPacket.name, serverId)

    connection.enableEncryption(secretKey)

    require(profile.uuid == loginStartPacket.playerUUID) { "UUID is not correct" }

    TODO("connect to backend")

//    connection.sendPacket(LoginSuccessPacket(profile.uuid, profile.name, profile.properties)).join()
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
        } else {
            continuation.resume(json.decodeFromString(response.body()))
        }
    }


}