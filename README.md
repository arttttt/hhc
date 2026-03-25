# HHC — HandHeld Controller

A Kotlin/Native application for Linux that bridges physical game controllers to a virtual Sony DualSense Edge controller via UHID. This allows games that support DualSense-specific features (adaptive triggers, touchpad, gyroscope) to work with other controllers.

## How It Works

```
Physical Controller (hidraw)
        |
        v
  InputMiddleware (custom mapping, macros)
        |
        v
  Virtual DualSense Edge (UHID)
        |
        v
  System sees a real DualSense
```

The application detects a physical controller, reads raw HID reports from it, processes input through a configurable middleware layer, and outputs DualSense Edge-compatible reports to a virtual UHID device.

## Supported Hardware

| Controller | Status |
|---|---|
| Lenovo Legion Go | Full support (buttons, sticks, triggers, extra buttons, rumble) |
| Xbox controllers | WIP |

## Requirements

- Linux with `/dev/uhid` access
- JDK 21 (for building)

## Building

```bash
./gradlew build
```

The native binary is produced at `build/bin/native/releaseExecutable/hhc.kexe`.

## Running

```bash
sudo ./build/bin/native/releaseExecutable/hhc.kexe
```

Root access (or appropriate udev rules) is required for `/dev/uhid` and `/dev/hidraw*` access.

## Documentation

See [docs/architecture.md](docs/architecture.md) for detailed architecture and component documentation.
