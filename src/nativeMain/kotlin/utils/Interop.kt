package utils

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
inline fun <reified T : CStructVar> Array<T>.toCArrayPointer(): CArrayPointer<T> {
    val firstElement = this@toCArrayPointer[0]

    return firstElement.ptr.reinterpret()
}