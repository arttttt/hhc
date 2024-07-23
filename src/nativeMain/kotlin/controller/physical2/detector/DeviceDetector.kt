package controller.physical2.detector

import controller.physical2.common.PhysicalController2

interface DeviceDetector {

    fun detect(): PhysicalController2?
}