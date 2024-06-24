import controller.bridge.GamepadBridgeImpl
import controller.physical.detector.ControllerDetectorImpl
import controller.physical.factory.PhysicalControllerFactory
import controller.physical.xbox.XboxController
import controller.virtual.dualsense.Dualsense
import platform.posix.sleep

fun main() {
    GamepadBridgeImpl(
        controllerDetector = ControllerDetectorImpl(
            factory = PhysicalControllerFactory(
                factories = mapOf(
                    (XboxController.Factory.vendor to XboxController.Factory.product) to XboxController.Factory
                )
            ),
        ),
        virtualControllerFactory = {
            Dualsense()
        }
    ).start()

    //val dualsense = Dualsense()

    //dualsense.create()

    //XboxController().start()

    // Clean up
    sleep(20000000u)

    //dualsense.destroy()
}
