package org.endoqa.fastly


import java.net.InetAddress

data class BackendServer(
    val name: String,
    val address: InetAddress,
    val port: Int
)