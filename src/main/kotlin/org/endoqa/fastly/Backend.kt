package org.endoqa.fastly

import java.net.InetAddress


private val ID_REGEX = Regex("[a-zA-Z0-9_]+")

data class Backend(
    val name: String,
    val address: String,
    val port: Int
) {

    init {
        require(name.matches(ID_REGEX)) { "Backend name must match $ID_REGEX" }
        require(runCatching { InetAddress.getByName(address) }.isSuccess) { "Backend address must be a valid IP address" }
        require(port in 1..65535) { "Backend port must be between 1 and 65535" }
    }

}