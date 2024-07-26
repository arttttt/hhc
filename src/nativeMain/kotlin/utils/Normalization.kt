package utils

import controller.common.normalization.NormalizationMode

fun normalize(
    value: Int,
    mode: NormalizationMode
): Double {
    return when (mode) {
        NormalizationMode.I8 -> normalizeI8(value)
        NormalizationMode.U8 -> normalizeU8(value)
        NormalizationMode.M8 -> normalizeM8(value)
        NormalizationMode.I16 -> normalizeI16(value)
        NormalizationMode.U16 -> normalizeU16(value)
        NormalizationMode.M16 -> normalizeM16(value)
        NormalizationMode.I32 -> normalizeI32(value)
        NormalizationMode.U32 -> normalizeU32(value)
        NormalizationMode.M32 -> normalizeM32(value)
    }
}

fun denormalize(
    value: Double,
    mode: NormalizationMode
): Int {
    return when (mode) {
        NormalizationMode.I8 -> denormalizeI8(value)
        NormalizationMode.U8 -> denormalizeU8(value)
        NormalizationMode.M8 -> denormalizeM8(value)
        NormalizationMode.I16 -> denormalizeI16(value)
        NormalizationMode.U16 -> denormalizeU16(value)
        NormalizationMode.M16 -> denormalizeM16(value)
        NormalizationMode.I32 -> denormalizeI32(value)
        NormalizationMode.U32 -> denormalizeU32(value)
        NormalizationMode.M32 -> denormalizeM32(value)
    }
}

fun convertNormalizedValue(value: Double, fromMode: NormalizationMode, toMode: NormalizationMode): Double {
    return normalize(
        value = denormalize(
            value = value,
            mode = fromMode,
        ),
        mode = toMode,
    )
}

private fun normalizeM8(value: Int): Double {
    val intValue = (value and 0xFF) - (1 shl 7)
    val s = (1 shl 7).toDouble()

    return intValue / s
}

private fun normalizeI8(value: Int): Double {
    val intValue = value.toByte().toInt() // Преобразуем к байту и обратно к int для знакового значения
    val s = (1 shl 7).toDouble() - 1

    return intValue / s
}

private fun normalizeU8(value: Int): Double {
    val intValue = value and 0xFF
    val s = (1 shl 8).toDouble() - 1

    return intValue / s
}

private fun normalizeI16(value: Int): Double {
    val intValue = (value and 0xFFFF).toShort().toInt() // Преобразуем к short и обратно к int для знакового значения
    val s = (1 shl 15).toDouble() - 1

    return intValue / s
}

private fun normalizeU16(value: Int): Double {
    val intValue = value and 0xFFFF
    val s = (1 shl 16).toDouble() - 1

    return intValue / s
}

private fun normalizeM16(value: Int): Double {
    val intValue = (value and 0xFFFF) - (1 shl 15)
    val s = (1 shl 15).toDouble() - 1

    return intValue / s
}

private fun normalizeI32(value: Int): Double {
    val intValue = value // Уже знаковое значение
    val s = (1 shl 31).toDouble() - 1

    return intValue / s
}

private fun normalizeU32(value: Int): Double {
    val intValue = value.toLong() and 0xFFFFFFFFL // Преобразуем к long для unsigned значения
    val s = (1L shl 32).toDouble() - 1

    return intValue / s
}

private fun normalizeM32(value: Int): Double {
    val intValue = (value.toLong() and 0xFFFFFFFFL) - (1 shl 31)
    val s = (1 shl 31).toDouble() - 1

    return intValue / s
}

private fun denormalizeM8(value: Double): Int {
    val s = (1 shl 7).toDouble()
    val intValue = (value * s).toInt() + (1 shl 7)

    return intValue and 0xFF
}

private fun denormalizeI8(value: Double): Int {
    val s = (1 shl 7).toDouble() - 1
    val intValue = (value * s).toInt()

    return intValue.toByte().toInt() and 0xFF
}

private fun denormalizeU8(value: Double): Int {
    val s = (1 shl 8).toDouble() - 1
    val intValue = (value * s).toInt()

    return intValue and 0xFF
}

private fun denormalizeI16(value: Double): Int {
    val s = (1 shl 15).toDouble() - 1
    val intValue = (value * s).toInt()

    return intValue.toShort().toInt() and 0xFFFF
}

private fun denormalizeU16(value: Double): Int {
    val s = (1 shl 16).toDouble() - 1
    val intValue = (value * s).toInt()

    return intValue and 0xFFFF
}

private fun denormalizeM16(value: Double): Int {
    val s = (1 shl 15).toDouble() - 1
    val intValue = (value * s).toInt() + (1 shl 15)

    return intValue and 0xFFFF
}

private fun denormalizeI32(value: Double): Int {
    val s = (1 shl 31).toDouble() - 1
    val intValue = (value * s).toInt()

    return intValue
}

private fun denormalizeU32(value: Double): Int {
    val s = (1L shl 32).toDouble() - 1
    val intValue = (value * s).toLong()

    return (intValue and 0xFFFFFFFFL).toInt()
}

private fun denormalizeM32(value: Double): Int {
    val s = (1 shl 31).toDouble() - 1
    val intValue = (value * s).toInt() + (1 shl 31)

    return intValue
}

private fun getNormalizationRange(mode: NormalizationMode): Pair<Double, Double> {
    return when (mode) {
        NormalizationMode.U8 -> 0.0 to 255.0
        NormalizationMode.I8 -> -127.0 to 127.0
        NormalizationMode.M8 -> -128.0 to 127.0
        NormalizationMode.U16 -> 0.0 to 65535.0
        NormalizationMode.I16 -> -32767.0 to 32767.0
        NormalizationMode.M16 -> -32768.0 to 32767.0
        NormalizationMode.U32 -> 0.0 to 4294967295.0
        NormalizationMode.I32 -> -2147483647.0 to 2147483647.0
        NormalizationMode.M32 -> -2147483648.0 to 2147483647.0
    }
}
