import controller.physical.factory.PhysicalControllerFactory
import controller.physical.detector.ControllerDetectorImpl
import controller.physical.xbox.XboxController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import platform.posix.sleep

fun main() {
    GamepadBridge(
        controllerDetector = ControllerDetectorImpl(
            factory = PhysicalControllerFactory(
                factories = mapOf(
                    (XboxController.Factory.vendor to XboxController.Factory.product) to XboxController.Factory
                )
            ),
        ),
    ).start()

    //val dualsense = Dualsense()

    //dualsense.create()

    //XboxController().start()

    // Clean up
    sleep(20000000u)

    //dualsense.destroy()
}
