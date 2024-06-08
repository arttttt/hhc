package controller.virtual.common

object MacAddressFormatter {
    fun format(macAddr: UByteArray): String {
        require(macAddr.size == 6) { "MAC address must be 6 bytes long" }
        return macAddr
            .reversed()
            .joinToString(":") { byte ->
                byte.toInt().and(0xFF).toString(16).padStart(2, '0')
            }
    }
}