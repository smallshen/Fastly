@file:JvmName("Main")

package org.endoqa.fastly.main

import com.google.protobuf.DynamicMessage
import dev.cel.common.CelProtoAbstractSyntaxTree
import dev.cel.expr.CheckedExpr
import dev.cel.runtime.CelRuntimeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.endoqa.fastly.Backend
import org.endoqa.fastly.FastlyServer
import org.tinylog.Logger
import java.io.File
import org.endoqa.fastly.config.BackendServerOuterClass.BackendServer as BackendServerProto


fun main(args: Array<String>) {
    return entry(args)
}

private fun entry(args: Array<String>) {

    val backendFunction = File("backends.cel.proto")


    val celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder()
        .addMessageTypes(BackendServerProto.getDescriptor())
        .build()

    val expr = CheckedExpr.parseFrom(backendFunction.readBytes())

    val ast = CelProtoAbstractSyntaxTree.fromCheckedExpr(expr).ast

    val program = celRuntime.createProgram(ast)

    val backends = mutableListOf<Backend>()

    fun BackendServerProto.asBackend(): Backend {
        return Backend(
            this.name,
            this.address,
            this.port,
        )
    }

    try {
        @Suppress("UNCHECKED_CAST")
        val config = program.eval() as List<DynamicMessage>

        config.forEach {
            backends += BackendServerProto.parseFrom(it.toByteArray()).asBackend()
        }

    } catch (e: Exception) {
        Logger.error(e, "Error reading config")
        return
    }

    val server = FastlyServer(
        args.getOrNull(0)?.toIntOrNull() ?: error("No port provided"),
        true,
        256,
        args.getOrNull(1) ?: error("No forward secret provided"),
    )

    server.backends.addAll(backends)

    runBlocking(Dispatchers.Default) {
        server.start()
    }

}