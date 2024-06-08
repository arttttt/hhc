import controller.virtual.dualsense.Dualsense
import platform.posix.sleep

fun main() {
    val dualsense = Dualsense()

    dualsense.create()

    // Clean up
    sleep(20000000u)

    dualsense.destroy()
}
