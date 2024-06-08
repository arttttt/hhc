@file:Suppress("ArrayInDataClass")

@ExperimentalUnsignedTypes
enum class PowerState(val value: UByte) {
    Discharging(0x00u),
    Charging(0x01u),
    Complete(0x02u),
    AbnormalVoltage(0x0Au),
    AbnormalTemperature(0x0Bu),
    ChargingError(0x0Fu);

    companion object {
        fun fromUByte(value: UByte): PowerState {
            return entries.find { it.value == value } ?: Discharging
        }
    }
}

@ExperimentalUnsignedTypes
enum class Direction(val value: UByte) {
    North(0u),
    NorthEast(1u),
    East(2u),
    SouthEast(3u),
    South(4u),
    SouthWest(5u),
    West(6u),
    NorthWest(7u),
    None(8u);

    companion object {
        fun fromUByte(value: UByte): Direction {
            return entries.find { it.value == value } ?: None
        }
    }
}

@ExperimentalUnsignedTypes
data class TouchFingerData(
    var xLo: UByte,
    var xHi: UByte,
    var yLo: UByte,
    var yHi: UByte
) {
    fun setX(xRaw: UShort) {
        xLo = (xRaw and 0x00FFu).toUByte()
        xHi = ((xRaw and 0x0F00u).rotateRight(8)).toUByte()
    }

    fun setY(yRaw: UShort) {
        yLo = (yRaw and 0x000Fu).toUByte()
        yHi = ((yRaw and 0x0FF0u).rotateRight(4)).toUByte()
    }
}

@ExperimentalUnsignedTypes
data class TouchData(
    val touchFingerData: Array<TouchFingerData>,
    val timestamp: UByte
)

@ExperimentalUnsignedTypes
data class USBPackedInputDataReport(
    val reportId: UByte,
    val joystickLX: UByte,
    val joystickLY: UByte,
    val joystickRX: UByte,
    val joystickRY: UByte,
    val l2Trigger: UByte,
    val r2Trigger: UByte,
    val seqNumber: UByte,
    val triangle: Boolean,
    val circle: Boolean,
    val cross: Boolean,
    val square: Boolean,
    val dpad: Direction,
    val r3: Boolean,
    val l3: Boolean,
    val options: Boolean,
    val create: Boolean,
    val r2: Boolean,
    val l2: Boolean,
    val r1: Boolean,
    val l1: Boolean,
    val rightPaddle: Boolean,
    val leftPaddle: Boolean,
    val rightFn: Boolean,
    val leftFn: Boolean,
    val unkn0: Boolean,
    val mute: Boolean,
    val touchpad: Boolean,
    val ps: Boolean,
    val unkn1: UByte,
    val unknCounter: UInt,
    val gyroX: Short,
    val gyroY: Short,
    val gyroZ: Short,
    val accelX: Short,
    val accelY: Short,
    val accelZ: Short,
    val sensorTimestamp: UInt,
    val temperature: UByte,
    val touchData: TouchData,
    val triggerLeftStatus: UByte,
    val triggerLeftStopLocation: UByte,
    val triggerRightStatus: UByte,
    val triggerRightStopLocation: UByte,
    val hostTimestamp: UInt,
    val triggerLeftEffect: UByte,
    val triggerRightEffect: UByte,
    val deviceTimestamp: UInt,
    val powerState: PowerState,
    val powerPercent: UByte,
    val pluggedUnkn0: UByte,
    val pluggedUsbPower: Boolean,
    val pluggedUsbData: Boolean,
    val micMutes: Boolean,
    val pluggedMic: Boolean,
    val pluggedHeadphones: Boolean,
    val pluggedUnkn1: UByte,
    val hapticLowPassFilter: Boolean,
    val pluggedExternalMic: Boolean,
    val aesCmac: ByteArray,
) {

    companion object
}

@ExperimentalUnsignedTypes
fun USBPackedInputDataReport.Companion.fromByteArray(data: UByteArray): USBPackedInputDataReport {
    require(data.size >= 64) { "Data array is too short" }

    return USBPackedInputDataReport(
        reportId = data[0],
        joystickLX = data[1],
        joystickLY = data[2],
        joystickRX = data[3],
        joystickRY = data[4],
        l2Trigger = data[5],
        r2Trigger = data[6],
        seqNumber = data[7],
        triangle = (data[8].toInt() and 0b10000000) != 0,
        circle = (data[8].toInt() and 0b01000000) != 0,
        cross = (data[8].toInt() and 0b00100000) != 0,
        square = (data[8].toInt() and 0b00010000) != 0,
        dpad = Direction.fromUByte((data[8].toInt() and 0b00001111).toUByte()),
        r3 = (data[9].toInt() and 0b10000000) != 0,
        l3 = (data[9].toInt() and 0b01000000) != 0,
        options = (data[9].toInt() and 0b00100000) != 0,
        create = (data[9].toInt() and 0b00010000) != 0,
        r2 = (data[9].toInt() and 0b00001000) != 0,
        l2 = (data[9].toInt() and 0b00000100) != 0,
        r1 = (data[9].toInt() and 0b00000010) != 0,
        l1 = (data[9].toInt() and 0b00000001) != 0,
        rightPaddle = (data[10].toInt() and 0b10000000) != 0,
        leftPaddle = (data[10].toInt() and 0b01000000) != 0,
        rightFn = (data[10].toInt() and 0b00100000) != 0,
        leftFn = (data[10].toInt() and 0b00010000) != 0,
        unkn0 = (data[10].toInt() and 0b00001000) != 0,
        mute = (data[10].toInt() and 0b00000100) != 0,
        touchpad = (data[10].toInt() and 0b00000010) != 0,
        ps = (data[10].toInt() and 0b00000001) != 0,
        unkn1 = data[11],
        unknCounter = (data[12].toUInt() shl 24) or (data[13].toUInt() shl 16) or (data[14].toUInt() shl 8) or data[15].toUInt(),
        gyroX = (data[16].toInt() or (data[17].toInt() shl 8)).toShort(),
        gyroY = (data[18].toInt() or (data[19].toInt() shl 8)).toShort(),
        gyroZ = (data[20].toInt() or (data[21].toInt() shl 8)).toShort(),
        accelX = (data[22].toInt() or (data[23].toInt() shl 8)).toShort(),
        accelY = (data[24].toInt() or (data[25].toInt() shl 8)).toShort(),
        accelZ = (data[26].toInt() or (data[27].toInt() shl 8)).toShort(),
        sensorTimestamp = (data[28].toUInt() shl 24) or (data[29].toUInt() shl 16) or (data[30].toUInt() shl 8) or data[31].toUInt(),
        temperature = data[32],
        touchData = TouchData(
            arrayOf(
                TouchFingerData(data[33], data[34], data[35], data[36]),
                TouchFingerData(data[37], data[38], data[39], data[40])
            ),
            data[41],
        ),
        triggerLeftStatus = data[42],
        triggerLeftStopLocation = data[43],
        triggerRightStatus = data[44],
        triggerRightStopLocation = data[45],
        hostTimestamp = (data[46].toUInt() shl 24) or (data[47].toUInt() shl 16) or (data[48].toUInt() shl 8) or data[49].toUInt(),
        triggerLeftEffect = data[50],
        triggerRightEffect = data[51],
        deviceTimestamp = (data[52].toUInt() shl 24) or (data[53].toUInt() shl 16) or (data[54].toUInt() shl 8) or data[55].toUInt(),
        powerState = PowerState.fromUByte((data[56].toInt() ushr 4).toUByte()),
        powerPercent = (data[56].toInt() and 0b00001111).toUByte(),
        pluggedUnkn0 = data[57],
        pluggedUsbPower = (data[58].toInt() and 0b10000000) != 0,
        pluggedUsbData = (data[58].toInt() and 0b01000000) != 0,
        micMutes = (data[58].toInt() and 0b00100000) != 0,
        pluggedMic = (data[58].toInt() and 0b00010000) != 0,
        pluggedHeadphones = (data[58].toInt() and 0b00001000) != 0,
        pluggedUnkn1 = data[59],
        hapticLowPassFilter = (data[60].toInt() and 0b01000000) != 0,
        pluggedExternalMic = (data[60].toInt() and 0b00100000) != 0,
        aesCmac = data.sliceArray(61 until 64).toByteArray() // исправлено на 64
    )
}

@ExperimentalUnsignedTypes
fun USBPackedInputDataReport.toByteArray(): UByteArray {
    val result = UByteArray(64)

    result[0] = this.reportId
    result[1] = this.joystickLX
    result[2] = this.joystickLY
    result[3] = this.joystickRX
    result[4] = this.joystickRY
    result[5] = this.l2Trigger
    result[6] = this.r2Trigger
    result[7] = this.seqNumber
    result[8] = ((if (this.triangle) 0b10000000 else 0) or
            (if (this.circle) 0b01000000 else 0) or
            (if (this.cross) 0b00100000 else 0) or
            (if (this.square) 0b00010000 else 0) or
            this.dpad.value.toInt()).toUByte()
    result[9] = ((if (this.r3) 0b10000000 else 0) or
            (if (this.l3) 0b01000000 else 0) or
            (if (this.options) 0b00100000 else 0) or
            (if (this.create) 0b00010000 else 0) or
            (if (this.r2) 0b00001000 else 0) or
            (if (this.l2) 0b00000100 else 0) or
            (if (this.r1) 0b00000010 else 0) or
            (if (this.l1) 0b00000001 else 0)).toUByte()
    result[10] = ((if (this.rightPaddle) 0b10000000 else 0) or
            (if (this.leftPaddle) 0b01000000 else 0) or
            (if (this.rightFn) 0b00100000 else 0) or
            (if (this.leftFn) 0b00010000 else 0) or
            (if (this.unkn0) 0b00001000 else 0) or
            (if (this.mute) 0b00000100 else 0) or
            (if (this.touchpad) 0b00000010 else 0) or
            (if (this.ps) 0b00000001 else 0)).toUByte()
    result[11] = this.unkn1
    result[12] = (this.unknCounter shr 24).toUByte()
    result[13] = (this.unknCounter shr 16).toUByte()
    result[14] = (this.unknCounter shr 8).toUByte()
    result[15] = this.unknCounter.toUByte()
    result[16] = (this.gyroX.toInt() shr 8).toUByte()
    result[17] = this.gyroX.toUByte()
    result[18] = (this.gyroY.toInt() shr 8).toUByte()
    result[19] = this.gyroY.toUByte()
    result[20] = (this.gyroZ.toInt() shr 8).toUByte()
    result[21] = this.gyroZ.toUByte()
    result[22] = (this.accelX.toInt() shr 8).toUByte()
    result[23] = this.accelX.toUByte()
    result[24] = (this.accelY.toInt() shr 8).toUByte()
    result[25] = this.accelY.toUByte()
    result[26] = (this.accelZ.toInt() shr 8).toUByte()
    result[27] = this.accelZ.toUByte()
    result[28] = (this.sensorTimestamp shr 24).toUByte()
    result[29] = (this.sensorTimestamp shr 16).toUByte()
    result[30] = (this.sensorTimestamp shr 8).toUByte()
    result[31] = this.sensorTimestamp.toUByte()
    result[32] = this.temperature
    result[33] = this.touchData.touchFingerData[0].xLo
    result[34] = this.touchData.touchFingerData[0].xHi
    result[35] = this.touchData.touchFingerData[0].yLo
    result[36] = this.touchData.touchFingerData[0].yHi
    result[37] = this.touchData.touchFingerData[1].xLo
    result[38] = this.touchData.touchFingerData[1].xHi
    result[39] = this.touchData.touchFingerData[1].yLo
    result[40] = this.touchData.touchFingerData[1].yHi
    result[41] = this.touchData.timestamp
    result[42] = this.triggerLeftStatus
    result[43] = this.triggerLeftStopLocation
    result[44] = this.triggerRightStatus
    result[45] = this.triggerRightStopLocation
    result[46] = (this.hostTimestamp shr 24).toUByte()
    result[47] = (this.hostTimestamp shr 16).toUByte()
    result[48] = (this.hostTimestamp shr 8).toUByte()
    result[49] = this.hostTimestamp.toUByte()
    result[50] = this.triggerLeftEffect
    result[51] = this.triggerRightEffect
    result[52] = (this.deviceTimestamp shr 24).toUByte()
    result[53] = (this.deviceTimestamp shr 16).toUByte()
    result[54] = (this.deviceTimestamp shr 8).toUByte()
    result[55] = this.deviceTimestamp.toUByte()
    result[56] = ((this.powerState.value.toInt() shl 4) or this.powerPercent.toInt()).toUByte()
    result[57] = this.pluggedUnkn0
    result[58] = ((if (this.pluggedUsbPower) 0b10000000 else 0) or
            (if (this.pluggedUsbData) 0b01000000 else 0) or
            (if (this.micMutes) 0b00100000 else 0) or
            (if (this.pluggedMic) 0b00010000 else 0) or
            (if (this.pluggedHeadphones) 0b00001000 else 0)).toUByte()
    result[59] = this.pluggedUnkn1
    result[60] = ((if (this.hapticLowPassFilter) 0b01000000 else 0) or
            (if (this.pluggedExternalMic) 0b00100000 else 0)).toUByte()
    this.aesCmac.forEachIndexed { index, byte ->
        result[61 + index] = byte.toUByte()
    }

    return result
}
