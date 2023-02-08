package org.endoqa.fastly.nio

import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private typealias AcceptHandler = CompletionHandler<AsynchronousSocketChannel, Unit?>

@JvmInline
value class AsyncServerSocket(private val channel: AsynchronousServerSocketChannel) {


    suspend fun accept() = suspendCoroutine<AsynchronousSocketChannel> { continuation ->
        channel.accept(
            null,
            object : AcceptHandler {
                override fun completed(result: AsynchronousSocketChannel, attachment: Unit?) {
                    continuation.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Unit?) {
                    continuation.resumeWithException(exc)
                }
            }
        )
    }

}