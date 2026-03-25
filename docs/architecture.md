# Architecture

## Overview

HHC follows a pipeline architecture where input flows from physical hardware through processing layers to a virtual controller output.

```
                          ┌─────────────────┐
                          │ ControllerDetector│
                          └────────┬─────────┘
                                   │ detects
                          ┌────────v─────────┐
                          │  GamepadBridge    │
                          │  (orchestrator)   │
                          └────────┬─────────┘
                                   │ connects
               ┌───────────────────┼───────────────────┐
               │                   │                   │
    ┌──────────v──────┐  ┌────────v────────┐  ┌──────v───────────┐
    │ PhysicalController│  │ InputMiddleware │  │ VirtualController │
    │ (hidraw reader)  │  │ (mapping/macros)│  │ (DualSense UHID) │
    └──────────┬──────┘  └────────┬────────┘  └──────┬───────────┘
               │                   │                   │
               └───────── poll() loop ─────────────────┘
```

## Startup Flow

1. `main()` creates a `GamepadBridgeImpl` with a detector and a virtual controller factory
2. Bridge launches a detection coroutine
3. `DeviceDetectorImpl` reads `/sys/devices/virtual/dmi/id/product_name` to identify the hardware
4. For known hardware (e.g. Legion Go), it scans `/dev/hidraw*` for matching VID/PID devices
5. Bridge calls `connectController()` which creates a virtual DualSense, an InputMiddleware, and starts the poll loop

## Runtime Pipeline

### Poll Loop

`GamepadBridgeImpl.startInputEventsLoop()` uses Linux `poll()` to monitor file descriptors from all three components:

- **Physical controller** fd — hidraw device
- **Virtual controller** fd — UHID device (for output reports like rumble)
- **InputMiddleware** fd — timerfd (for scheduled macro events)

When data is available on any fd, the corresponding component's `readEvents()` is called.

### Data Flow

```
hidraw device
    │
    │  raw bytes
    v
PhysicalController.processInputData()
    │
    │  parses buttons (bit positions) and axes (byte offsets)
    │  updates ControllerState
    │
    │  onControllerStateChanged callback
    v
InputMiddleware.consumeControllerState()
    │
    │  applies custom mappings, schedules macro events
    │  merges internal state with external state
    │
    │  onControllerStateChanged callback
    v
Dualsense.consumeControllerState()
    │
    │  converts ButtonCode/AxisCode state to DualSense report format
    │  normalizes values between source and target formats
    │
    │  UHidEvent.Input(64-byte report)
    v
/dev/uhid → system sees DualSense Edge
```

## Key Abstractions

### Controller

`Controller` interface — base for both physical and virtual controllers:
- `controllerState: ControllerState` — current state
- `readEvents()` — reads input from the device
- `consumeControllerState(state)` — accepts state from another controller
- `onControllerStateChanged` — callback when state changes

### Input State

Input state is composed via delegation:
- `ButtonsStateOwner` — manages button states (pressed/released)
- `AxisStateOwner` — manages axis values (sticks, triggers)

Both support two modes of input:
- **By system code** — from evdev events (`EV_KEY`/`EV_ABS` codes)
- **By raw report** — from hidraw byte arrays using bit/byte positions from mappings

### Mappings

`ButtonMapping` defines how a button maps between physical and virtual:
- `systemCode` — Linux input code (e.g. `BTN_A`) or `UNKNOWN_SYSTEM_CODE` for vendor-specific
- `code` — internal `ButtonCode` enum
- `location` — bit position in the raw HID report

`AxisMapping` is similar:
- `systemCode` — Linux axis code (e.g. `ABS_X`)
- `code` — internal `AxisCode` enum
- `location` — byte position in the raw HID report
- `normalizationMode` — how to interpret the raw value

### Normalization

Values flow between different numeric formats as they move through the pipeline. The normalization system handles this:

| Mode | Range | Use case |
|------|-------|----------|
| U8 | 0..255 → 0.0..1.0 | DualSense sticks, triggers |
| I8 | -127..127 → -1.0..1.0 | Signed byte axes |
| M8 | -128..127 → -1.0..1.0 | Mapped byte axes (hidraw sticks) |
| U16/I16/M16 | 16-bit variants | Evdev axes |

`convertNormalizedValue()` handles direct conversion between signed and unsigned domains without round-tripping through integers.

## Physical Controllers

### Lenovo Legion Go

`LenovoLegionGoController` reads from hidraw device(s) with VID `0x17EF`, PID `0x6182`:
- 20 buttons mapped to specific bit positions in the HID report (byte 18-20)
- 6 axes: LX, LY, RX, RY (M8 at bytes 14-17), LT, RT (U8 at bytes 22-23)
- Report ID: `0x04`
- Supports both evdev (for standard inputs) and hidraw (for vendor-specific data)

### Xbox Controllers

`XboxController` — WIP, basic structure in place.

## Virtual Controllers

### DualSense Edge

`Dualsense` creates a virtual Sony DualSense Edge controller:
- VID `0x054C`, PID `0x0DF2`
- 64-byte USB input reports (Report ID `0x01`)
- D-pad encoded as direction enum (8 directions + none)
- Responds to feature report requests: pairing info, firmware info, calibration
- Processes output reports for rumble feedback

## InputMiddleware

Sits between physical and virtual controllers:
- Custom button remapping (e.g. Share button triggers a sequence)
- Timed event scheduling via `timerfd` and a `PriorityQueue`
- Merges its own internal state with external controller state

## Linux APIs Used

| API | Device path | Purpose |
|-----|-------------|---------|
| **hidraw** | `/dev/hidrawN` | Read raw HID reports from physical controllers |
| **UHID** | `/dev/uhid` | Create virtual HID devices |
| **ioctl** | — | Device info, exclusive grab (`EVIOCGRAB`), HID report descriptors |
| **poll()** | — | Event loop for multiple file descriptors |
| **timerfd** | — | Precise timer scheduling for input macros |
| **force feedback** | — | Rumble effects on physical controllers (`FF_RUMBLE`) |

## Package Layout

```
src/nativeMain/kotlin/
├── Main.kt                          # Entry point
├── controller/
│   ├── bridge/                      # GamepadBridge — orchestration
│   ├── common/                      # Shared interfaces
│   │   ├── input/axis/              # Axis abstractions and state
│   │   ├── input/buttons/           # Button abstractions and state
│   │   ├── normalization/           # NormalizationMode enum
│   │   └── rumble/                  # Rumble handling + FF_RUMBLE
│   ├── physical2/                   # Physical controller implementations
│   │   ├── common/                  # Base classes, InputDevice, mappings
│   │   ├── detector/                # Hardware detection
│   │   ├── lego/                    # Lenovo Legion Go
│   │   └── xbox/                    # Xbox (WIP)
│   ├── virtual/                     # Virtual controller implementations
│   │   ├── common/                  # Base classes, UHID integration
│   │   └── dualsense/              # DualSense Edge emulation + constants
│   └── InputMiddleware.kt           # Custom input processing
├── uhid/                            # UHidDevice, UHidEvent wrappers
└── utils/                           # Normalization, ioctl helpers, PriorityQueue
```
