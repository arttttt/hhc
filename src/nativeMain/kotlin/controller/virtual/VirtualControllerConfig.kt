package controller.virtual

data class VirtualControllerConfig(
    val name: String,
    val uniq: String,
    val product: UInt,
    val vendor: UInt,
    val version: UInt,
    val country: UInt,
    val bus: UShort,
    val reportDescriptor: UByteArray,
)