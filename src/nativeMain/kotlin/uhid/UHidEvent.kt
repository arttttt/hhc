package uhid

import dualsense.constants.DS_OUTPUT_REPORT_USB
import kotlinx.cinterop.*
import platform.posix.memcpy

sealed interface UHidEvent {

    companion object;

    data class Create(
        val name: String,
        val uniq: String,
        val reportDescriptor: UByteArray,
        val bus: UShort,
        val product: UInt,
        val vendor: UInt,
        val version: UInt,
        val country: UInt,
    ) : UHidEvent

    data object Start : UHidEvent
    data object Open : UHidEvent
    data object Close : UHidEvent
    data object Stop : UHidEvent

    data class GetReport(
        val kind: UByte,
        val id: UInt,
    ) : UHidEvent

    data class GetReportReply(
        val size: UShort,
        val id: UInt,
        val err: UShort,
        val data: UByteArray,
    ) : UHidEvent

    data object Destroy : UHidEvent

    data class Output(
        val data: UByteArray,
    ) : UHidEvent

    data class Input(
        val size: UShort,
    ) : UHidEvent
}

fun UHidEvent.Companion.fromPlatformEvent(event: uhid_event): UHidEvent {
    return when (event.type) {
        uhid_event_type.UHID_START.value -> UHidEvent.Start
        uhid_event_type.UHID_OPEN.value -> UHidEvent.Open
        uhid_event_type.UHID_CLOSE.value -> UHidEvent.Close
        uhid_event_type.UHID_STOP.value -> UHidEvent.Stop
        uhid_event_type.UHID_GET_REPORT.value -> UHidEvent.GetReport(
            kind = event.u.get_report.rnum,
            id = event.u.get_report.id,
        )
        uhid_event_type.UHID_OUTPUT.value -> {
            UHidEvent.Output(
                data = event.u.output.data.readBytes(event.u.output.size.toInt()).toUByteArray()
            )
        }
        else -> throw IllegalStateException("unsupported event: ${event.type}")
    }
}

fun UHidEvent.toPlatformEvent(memScope: MemScope): uhid_event {
    return with(memScope) {
        when (this@toPlatformEvent) {
            is UHidEvent.Create -> alloc<uhid_event>().apply {
                type = uhid_event_type.UHID_CREATE2.value
                u.create2.apply {
                    memcpy(
                        name,
                        this@toPlatformEvent.name.cstr.getPointer(this@with),
                        this@toPlatformEvent.name.length.toULong()
                    )
                    memcpy(
                        uniq,
                        this@toPlatformEvent.uniq.cstr.getPointer(this@with),
                        this@toPlatformEvent.uniq.length.toULong()
                    )

                    rd_size = reportDescriptor.size.toUShort()
                    memcpy(rd_data, reportDescriptor.refTo(0), reportDescriptor.size.toULong())
                    bus = this@toPlatformEvent.bus
                    vendor = this@toPlatformEvent.vendor
                    product = this@toPlatformEvent.product
                    version = this@toPlatformEvent.version
                    country = this@toPlatformEvent.country
                }
            }

            is UHidEvent.Destroy -> alloc<uhid_event>().apply {
                type = uhid_event_type.UHID_DESTROY.value
            }

            is UHidEvent.GetReportReply -> alloc<uhid_event>().apply {
                type = uhid_event_type.UHID_GET_REPORT_REPLY.value
                u.get_report_reply.apply {
                    size = this@toPlatformEvent.size
                    id = this@toPlatformEvent.id
                    err = this@toPlatformEvent.err

                    memcpy(data, this@toPlatformEvent.data.refTo(0), this@toPlatformEvent.data.size.toULong())
                }
            }

            is UHidEvent.Input -> alloc<uhid_event>().apply {
                type = uhid_event_type.UHID_INPUT2.value
                u.input2.size = this@toPlatformEvent.size
            }

            else -> throw IllegalStateException("unsupported event: ${this@toPlatformEvent::class.simpleName}")
        }
    }
}