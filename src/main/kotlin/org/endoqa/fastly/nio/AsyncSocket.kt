package org.endoqa.fastly.nio

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private typealias ReadHandler = CompletionHandler<Int, Unit?>
private typealias WriteHandler = CompletionHandler<Int, Unit?>
private typealias ConnectHandler = CompletionHandler<Void, Unit?>

@JvmInline
value class AsyncSocket(val channel: AsynchronousSocketChannel) {

    suspend fun connect(addr: SocketAddress) {
        suspendCancellableCoroutine { cancellableContinuation ->

            cancellableContinuation.invokeOnCancellation {
                channel.close()
            }

            channel.connect(
                addr,
                null,
                object : ConnectHandler {
                    override fun completed(result: Void?, attachment: Unit?) {
                        cancellableContinuation.resume(Unit)
                    }

                    override fun failed(exc: Throwable, attachment: Unit?) {
                        cancellableContinuation.resumeWithException(exc)
                    }

                }
            )


        }

    }


    suspend fun readFully(cap: Int) = readFully(ByteBuffer.allocate(cap))

    suspend fun readFully(buf: ByteBuffer): ByteBuffer {
        while (buf.hasRemaining()) {
            read(buf)
        }

        return buf
    }

    suspend fun read(cap: Int) = read(ByteBuffer.allocate(cap))

    suspend fun read(buf: ByteBuffer): ByteBuffer = suspendCoroutine { continuation ->
        try {
            channel.read(
                buf,
                null,
                object : ReadHandler {
                    override fun completed(result: Int, attachment: Unit?) {
                        // empty
                        when (result) {
                            -1 -> {
                                channel.close()
                                continuation.resumeWithException(AsynchronousCloseException())
                            }

                            0 -> continuation.resumeWithException(RuntimeException("Connection closed(? maybe) because result is 0"))
                            else -> continuation.resume(buf)
                        }
                    }

                    override fun failed(exc: Throwable, attachment: Unit?) {
                        channel.close()
                        continuation.resumeWithException(exc)
                    }

                }
            )
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
        }
    }


    suspend fun write(buf: ByteBuffer) = suspendCoroutine<Unit> { continuation ->

        try {
            channel.write(
                buf,
                null,
                object : WriteHandler {
                    override fun completed(result: Int, attachment: Unit?) {
                        if (result == -1) {
                            continuation.resumeWithException(RuntimeException("Connection closed because result is -1"))
                        } else {
                            continuation.resume(Unit)
                        }
                    }

                    override fun failed(exc: Throwable, attachment: Unit?) {
                        continuation.resumeWithException(exc)
                    }

                }
            )
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
        }
    }


    fun close() {
        channel.close()
    }

}