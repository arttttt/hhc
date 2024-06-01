import dualsense.Dualsense
import kotlinx.cinterop.*
import platform.posix.*
import uhid.uhid_event
import uhid.uhid_event_type

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val dualsense = Dualsense()

    dualsense.create()

    // Clean up
    sleep(20000000u)

    dualsense.destroy()
}
