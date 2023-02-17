import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.endoqa.fastly.BackendServer
import org.endoqa.fastly.FastlyServer
import java.net.InetAddress

fun main() {
    val server = FastlyServer(25565, true, "123abc")

    server.backendServers += BackendServer(
        "test1",
        InetAddress.getByName("192.168.0.161"),
        25566,
    )

    runBlocking(Dispatchers.Default) {
        server.start()
    }
}