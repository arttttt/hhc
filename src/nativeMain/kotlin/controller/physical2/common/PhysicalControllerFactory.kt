package controller.physical2.common

interface PhysicalControllerFactory {

    fun create(devices: List<InputDevice>): PhysicalController2
}
