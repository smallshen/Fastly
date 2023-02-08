package org.endoqa.fastly.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.endoqa.fastly.nio.AsyncSocket
import org.endoqa.fastly.nio.ByteBuf
import org.endoqa.fastly.nio.ComplexAsyncSocket
import org.endoqa.fastly.protocol.MinecraftPacket
import org.endoqa.fastly.protocol.RawPacket
import org.endoqa.fastly.util.calculateVarIntSize
import java.nio.ByteBuffer


class Connection(val socket: AsyncSocket, val parentJob: Job? = null) : CoroutineScope {

    override val coroutineContext = Job(parentJob)

    val packetIn = Channel<RawPacket>()
    val packetOut = Channel<RawPacket>()

    init {
        coroutineContext.invokeOnCompletion {
            socket.close()
        }
    }

    fun startIO() {
        launch {
            incomingLoop()
        }
        launch {
            outgoingLoop()
        }
    }

    private suspend fun incomingLoop() {
        while (coroutineContext.isActive) {
            if (!socket.channel.isOpen) {
                coroutineContext.cancelAndJoin()
                break
            }

            packetIn.send(readRawPacket())
        }

    }

    private suspend fun outgoingLoop() {
        for (p in packetOut) {
            if (socket.channel.isOpen) {
                writeRawPacket(p)
                p.attachJob?.complete()
            } else {
                coroutineContext.cancelAndJoin()
                return
            }
        }
    }

    private suspend fun readRawPacket(): RawPacket {
        val cs = ComplexAsyncSocket(socket.channel)
        val length = cs.readVarInt()
        val packetId = cs.readVarInt()
        val actualDataLength = length - calculateVarIntSize(packetId)
        return RawPacket(length, packetId, socket.readFully(actualDataLength).position(0))
    }

    private suspend fun writeRawPacket(packet: RawPacket) {
        val (length, packetId, buffer) = packet

        val cs = ComplexAsyncSocket(socket.channel)
        cs.writeVarInt(length)
        cs.writeVarInt(packetId)
        socket.write(buffer.position(0))
    }

    suspend fun sendPacket(p: MinecraftPacket): CompletableJob {
        val buf = ByteBuffer.allocate(p.estimateSize())

        @Suppress("TYPE_MISMATCH")
        p.handler.write(ByteBuf(buf), p)
        buf.flip()

        val job = Job(coroutineContext)

        packetOut.send(RawPacket(buf.limit() + p.handler.packetIdSize, p.handler.packetId, buf, job))

        return job
    }

    fun close() {
        socket.channel.close()
        this.coroutineContext.complete()
    }
}