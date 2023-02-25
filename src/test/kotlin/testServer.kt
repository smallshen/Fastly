import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.endoqa.fastly.Backend
import org.endoqa.fastly.FastlyServer

fun main() {
    val server = FastlyServer(25565, true, 256, "123abc")

    server.backends += Backend(
        "test1",
        "192.168.0.161",
        25566,
    )

    runBlocking(Dispatchers.Default) {
        server.start()
    }
}