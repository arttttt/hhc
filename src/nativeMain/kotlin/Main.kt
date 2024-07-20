import controller.bridge.GamepadBridgeImpl
import controller.physical.detector.CompositeDeviceDetector
import controller.physical.detector.HidrawControllerDetector
import controller.physical.detector.StandardControllerDetector
import controller.physical.factory.PhysicalControllerFactory
import controller.physical.xbox.XboxController
import controller.virtual.dualsense.Dualsense
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.staticCFunction
import platform.posix.*

@Suppress("UnusedReceiverParameter", "RemoveRedundantQualifierName")
fun SignalHandler.registerSignals() {
    signal(
        SIGPOLL,
        staticCFunction { _: Int ->
            SignalHandler.handleSignal(SIGPOLL)
        },
    )
    signal(
        SIGINT,
        staticCFunction { _: Int ->
            SignalHandler.handleSignal(SIGINT)
        },
    )
    signal(
        SIGTERM,
        staticCFunction { _: Int ->
            SignalHandler.handleSignal(SIGTERM)
        },
    )
}

object SignalHandler {

    private val shouldExit = atomic(false)

    fun waitForSignal() {
        while (!shouldExit.value) {
            sleep(100u)
        }
    }

    fun handleSignal(signal: Int) {
        println("Received signal: $signal")
        shouldExit.value = true
    }
}

fun main() {
    val factory = PhysicalControllerFactory(
        factories = mapOf(
            (XboxController.Factory.vendor to XboxController.Factory.product) to XboxController.Factory,
        )
    )

    val gamepadBridge = GamepadBridgeImpl(
/*        controllerDetector = ControllerDetectorImpl(
            factory = PhysicalControllerFactory(
                factories = mapOf(
                    (XboxController.Factory.vendor to XboxController.Factory.product) to XboxController.Factory,
                    (LenovoLegionGoController.Factory.vendor to LenovoLegionGoController.Factory.product) to LenovoLegionGoController.Factory,
                )
            ),
        ),*/
        controllerDetector = CompositeDeviceDetector(
            hidrawDetector = HidrawControllerDetector(
                factory = factory,
            ),
            standardDetector = StandardControllerDetector(
                factory = factory,
            ),
        ),
        virtualControllerFactory = {
            Dualsense()
        }
    )

    gamepadBridge.start()

    SignalHandler.registerSignals()
    SignalHandler.waitForSignal()

    gamepadBridge.shutdown()
}
