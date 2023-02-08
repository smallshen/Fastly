package org.endoqa.fastly

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.nio.AsyncServerSocket
import org.endoqa.fastly.nio.AsyncSocket
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel

class FastlyServer(
    private val port: Int,
    val online: Boolean = false, //TODO: default to true in the future
) : CoroutineScope {

    override val coroutineContext = SupervisorJob()

    private val serverSocket = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(port))!!
    private val wrapper = AsyncServerSocket(serverSocket)

    val backendServers = mutableSetOf<BackendServer>()


    suspend fun start() {
        while (true) {
            val channel = wrapper.accept()
            doWork(channel)
        }
    }

    private fun doWork(channel: AsynchronousSocketChannel) {
        launch {
            handleConnection(channel)
        }
    }

    private suspend fun handleConnection(channel: AsynchronousSocketChannel) {
        val channelWrapper = AsyncSocket(channel)
        val c = Connection(channelWrapper, this.coroutineContext)
        c.startIO()

        val nextLogin = processHandshake(c) ?: return

        handleLogin(c, nextLogin)
    }


}