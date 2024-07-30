package controller

import controller.common.Controller
import controller.common.ControllerState
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.common.input.buttons.ButtonsStateOwnerImpl
import controller.physical2.common.ButtonMapping
import kotlinx.cinterop.*
import platform.linux.TFD_NONBLOCK
import platform.linux.timerfd_create
import platform.linux.timerfd_settime
import platform.posix.*
import utils.PriorityQueue

class InputMiddleware : Controller {

    class InputState : ControllerState,
        ButtonsStateOwner by ButtonsStateOwnerImpl(
            buttonsMapping = listOf(
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.SHARE,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.MODE,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.A,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
            ),
        )

    private data class ScheduledEvent(
        val buttonCode: ButtonCode,
        val isPressed: Boolean,
        val eventTime: EventTime,
    )

    private sealed interface EventTime : Comparable<EventTime> {

        val timeNs: Long

        override fun compareTo(other: EventTime): Int {
            return timeNs.compareTo(other.timeNs)
        }

        data object NONE : EventTime {

            override val timeNs: Long = 0
        }

        data class Offset(
            override val timeNs: Long,
        ) : EventTime
    }

    companion object {

        private const val SEND_A_DELAY = 150_000_000 // 150 миллисекунд в наносекундах
    }

    private var timerFd: Int = timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK)
    private val eventQueue = PriorityQueue<ScheduledEvent> { a, b -> a.eventTime.compareTo(b.eventTime) }

    private var lastSetTimer: EventTime = EventTime.NONE

    private var pollfd: pollfd? = null

    override var controllerState = InputState()

    override var onControllerStateChanged: ((ControllerState) -> Unit)? = null

    context(MemScope)
    fun start(): pollfd {
        val pollfd =  alloc<pollfd>().apply {
            fd = timerFd
            events = POLLIN.toShort()
        }

        this@InputMiddleware.pollfd = pollfd

        return pollfd
    }

    fun cleanup() {
        if (timerFd != -1) {
            close(timerFd)
            timerFd = -1
        }
    }

    context(MemScope)
    override fun readEvents() {
        val pollfd = pollfd ?: return

        if (pollfd.revents.toInt() and POLLIN != 0) {
            processInput(
                physicalState = null,
                currentTimeNs = getCurrentTimeNs(),
            )

            onControllerStateChanged?.invoke(controllerState)
        }
    }

    override fun consumeControllerState(state: ControllerState) {
        processInput(
            physicalState = state,
            currentTimeNs = getCurrentTimeNs(),
        )

        mergeStates(
            externalState = state,
        )

        onControllerStateChanged?.invoke(state)
    }

    private fun processInput(
        physicalState: ControllerState?,
        currentTimeNs: Long,
    ) {
        if (physicalState is ButtonsStateOwner) {
            handleShareButton(physicalState, currentTimeNs)
        }

        checkPendingEvents(
            currentTimeNs = currentTimeNs,
        )
    }

    private fun handleShareButton(
        physicalState: ButtonsStateOwner,
        currentTimeNs: Long,
    ) {
        if (!eventQueue.isEmpty) return
        val shareButton = physicalState.buttonsState[ButtonCode.SHARE] ?: return

        when {
            shareButton.isPressed && controllerState.buttonsState[ButtonCode.SHARE]?.isPressed == false -> {
                scheduleEvent(ButtonCode.SHARE, true, EventTime.Offset(currentTimeNs))
                scheduleEvent(ButtonCode.MODE, true, EventTime.Offset(currentTimeNs))
                scheduleEvent(ButtonCode.A, true, EventTime.Offset(currentTimeNs + SEND_A_DELAY))
            }
            !shareButton.isPressed && controllerState.buttonsState[ButtonCode.SHARE]?.isPressed == true -> {
                scheduleEvent(ButtonCode.SHARE, false, EventTime.Offset(currentTimeNs + SEND_A_DELAY))
                scheduleEvent(ButtonCode.MODE, false, EventTime.Offset(currentTimeNs + SEND_A_DELAY))
                scheduleEvent(ButtonCode.A, false, EventTime.Offset(currentTimeNs + SEND_A_DELAY))
            }
        }
    }

    private fun scheduleEvent(
        buttonCode: ButtonCode,
        isPressed: Boolean,
        eventTime: EventTime,
    ) {
        eventQueue.offer(ScheduledEvent(buttonCode, isPressed, eventTime))
    }

    private fun processScheduledEvents(currentTimeNs: Long) {
        if (eventQueue.isEmpty) return

        var processed = false

        while (!eventQueue.isEmpty && (eventQueue.peek()?.eventTime?.timeNs ?: Long.MAX_VALUE) <= currentTimeNs) {
            val event = eventQueue.poll() ?: break

            controllerState.setButtonState(event.buttonCode, event.isPressed)

            processed = true
        }

        if (processed) {
            checkPendingEvents(
                currentTimeNs = currentTimeNs,
            )
        }
    }

    private fun checkPendingEvents(
        currentTimeNs: Long,
    ) {
        if (eventQueue.isEmpty) {
            disableTimer()
        } else {
            val nextEvent = eventQueue.peek() ?: return

            if (nextEvent.needToProcess(currentTimeNs)) {
                processScheduledEvents(currentTimeNs)
                checkPendingEvents(currentTimeNs)
            } else {
                setTimer(nextEvent.eventTime.timeNs)
                lastSetTimer = nextEvent.eventTime
            }
        }
    }

    private fun setTimer(timeNs: Long) {
        memScoped {
            val newValue = alloc<itimerspec>()
            val currentTime = alloc<timespec>()
            clock_gettime(CLOCK_MONOTONIC, currentTime.ptr)

            val seconds = timeNs / 1_000_000_000
            val nanoseconds = timeNs % 1_000_000_000

            newValue.it_value.tv_sec = seconds - currentTime.tv_sec
            newValue.it_value.tv_nsec = nanoseconds - currentTime.tv_nsec

            if (newValue.it_value.tv_nsec < 0) {
                newValue.it_value.tv_sec--
                newValue.it_value.tv_nsec += 1_000_000_000
            }

            if (newValue.it_value.tv_sec < 0 || (newValue.it_value.tv_sec == 0L && newValue.it_value.tv_nsec <= 0)) {
                newValue.it_value.tv_sec = 0
                newValue.it_value.tv_nsec = 1
            }

            newValue.it_interval.tv_sec = 0
            newValue.it_interval.tv_nsec = 0

            if (timerfd_settime(timerFd, 0, newValue.ptr, null) == -1) {
                throw RuntimeException("Failed to set timer: ${strerror(errno)?.toKString()}")
            }
        }
    }

    private fun disableTimer() {
        if (lastSetTimer == EventTime.NONE) return

        memScoped {
            val newValue = alloc<itimerspec>()
            newValue.it_value.tv_sec = 0
            newValue.it_value.tv_nsec = 0
            newValue.it_interval.tv_sec = 0
            newValue.it_interval.tv_nsec = 0

            if (timerfd_settime(timerFd, 0, newValue.ptr, null) == -1) {
                throw RuntimeException("Failed to disable timer: ${strerror(errno)?.toKString()}")
            }
        }
        lastSetTimer = EventTime.NONE
    }

    private fun getCurrentTimeNs(): Long {
        memScoped {
            val timeSpec = alloc<timespec>()
            clock_gettime(CLOCK_MONOTONIC, timeSpec.ptr)
            return timeSpec.tv_sec * 1_000_000_000L + timeSpec.tv_nsec
        }
    }

    private fun mergeStates(externalState: ControllerState) {

        if (externalState is ButtonsStateOwner && controllerState.buttonsState[ButtonCode.SHARE]?.isPressed == true) {
            controllerState.buttonsState.forEach { (code, button) ->
                externalState.setButtonState(code, button.isPressed)
            }
        }
    }

    private fun ScheduledEvent.needToProcess(
        timestampNs: Long,
    ): Boolean {
        return when (eventTime) {
            is EventTime.NONE -> false
            is EventTime.Offset -> timestampNs >= eventTime.timeNs
        }
    }
}