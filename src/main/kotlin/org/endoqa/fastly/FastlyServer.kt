package org.endoqa.fastly

import kotlinx.coroutines.*
import org.endoqa.fastly.connection.Connection
import org.endoqa.fastly.nio.AsyncServerSocket
import org.endoqa.fastly.nio.AsyncSocket
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.spec.SecretKeySpec

class FastlyServer(
    private val port: Int,
    val online: Boolean = false, //TODO: default to true in the future
    forwardSecret: String
) : CoroutineScope {

    override val coroutineContext = SupervisorJob()

    private val serverSocket = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(port))!!
    private val wrapper = AsyncServerSocket(serverSocket)

    val backendServers = mutableSetOf<BackendServer>()

    val keyPair: KeyPair

    init {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024)
        keyPair = keyGen.generateKeyPair()
    }

    val modernForwardKey: Key = SecretKeySpec(forwardSecret.toByteArray(), "HmacSHA256")


    suspend fun start() {
        while (isActive) {
            try {
                val channel = wrapper.accept()
                doWork(channel)
            } catch (e: Exception) {
                e.printStackTrace() //TODO: logging here
            }
        }
    }

    private fun doWork(channel: AsynchronousSocketChannel) {
        launch {
            val channelWrapper = AsyncSocket(channel)

            val c = Connection(channelWrapper, coroutineContext.job)
            c.startIO()

            try {
                handleConnection(c)
                c.coroutineContext.join()
            } catch (e: Exception) {
                e.printStackTrace()
                c.cancel()
            }

            yield()
            c.coroutineContext.join() // no memory leaks, i guess
        }
    }

    private suspend fun handleConnection(connection: Connection) {

        val nextLogin = handleHandshake(connection) ?: return

        val playerConnection = if (online) {
            handleOnlineLogin(connection, nextLogin)
        } else {
            handleOfflineLogin(connection, nextLogin)
        }

        playerConnection.packetProxy()
    }


}