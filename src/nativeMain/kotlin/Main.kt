import controller.bridge.GamepadBridgeImpl
import controller.common.ControllerState
import controller.physical.InputDeviceHwInfo
import controller.physical.InputDeviceIds
import controller.physical.common.PhysicalController
import controller.physical.detector.CompositeDeviceDetector
import controller.physical.detector.HidrawControllerDetector
import controller.physical.detector.StandardControllerDetector
import controller.physical.factory.ControllerFactory
import controller.physical.factory.PhysicalControllerFactory
import controller.physical.lego.LenovoLegionGoController
import controller.physical.xbox.XboxController
import controller.physical2.detector.ControllerDetector2Impl
import controller.virtual.dualsense.Dualsense
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
            XboxController.Factory.ids to XboxController.Factory,
        )
    )

    val gamepadBridge = GamepadBridgeImpl(
        controllerDetector = ControllerDetector2Impl(),
        virtualControllerFactory = {
            Dualsense()
        }
    )

    gamepadBridge.start()

    SignalHandler.registerSignals()
    SignalHandler.waitForSignal()

    gamepadBridge.shutdown()
}
