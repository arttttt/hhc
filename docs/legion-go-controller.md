# Lenovo Legion Go Controller

## Device Identification

- **VID**: `0x17EF` (Lenovo)
- **PID**: `0x6182` (XInput mode)
- **Hardware model**: 83E1

## HID Interfaces

The controller presents as a USB composite device with multiple HID interfaces:

| Interface | Report ID | Size | Function |
|-----------|-----------|------|----------|
| 0x01 | `0x01` | 20 bytes | Touchpad (multi-touch, 3 contacts) |
| 0x02 | `0x04` | 64 bytes | XInput gamepad (all buttons, axes, IMU) |

Additional evdev devices: Touchpad, Keyboard (macro keys), Mouse.

## XInput Report Format

Report ID `0x04`, 64 bytes total. This is the primary data source via hidraw.

### Header & Status

| Byte | Field | Values |
|------|-------|--------|
| 0 | Report ID | `0x04` |
| 1 | Size | `0x3C` (60) |
| 2 | Command | `0x74` |
| 5 | Left battery | 0-100 |
| 6 | Left dock mode | 4=docked, 1=wireless |
| 7 | Right battery | 0-100 |
| 8 | Right dock mode | 4=docked, 1=wireless |
| 9 | Gamepad mode | 0=XInput |
| 12 | Left controller state | 2=docked, 3=wireless |
| 13 | Right controller state | 2=docked, 3=wireless |

### Analog Axes

| Byte | Field | Format | Range |
|------|-------|--------|-------|
| 14 | Left Stick X | U8 | 0-255, center=0x80 |
| 15 | Left Stick Y | U8 | 0-255, center=0x80 |
| 16 | Right Stick X | U8 | 0-255, center=0x80 |
| 17 | Right Stick Y | U8 | 0-255, center=0x80 |
| 22 | Left Trigger | U8 | 0-255 |
| 23 | Right Trigger | U8 | 0-255 |
| 25 | Mouse Wheel | I8 | -128..127 (scroll delta) |

### Button Bitfields

Bits numbered MSB-first (bit 7 = MSB).

**Byte 18:**

| Bit | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
|-----|---|---|---|---|---|---|---|---|
| | Legion | QuickAccess | ThumbL | ThumbR | DPadUp | DPadDown | DPadLeft | DPadRight |

**Byte 19:**

| Bit | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
|-----|---|---|---|---|---|---|---|---|
| | A | B | X | Y | LB | DTriggerL | RB | DTriggerR |

**Byte 20:**

| Bit | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
|-----|---|---|---|---|---|---|---|---|
| | Y1 | Y2 | Y3 | (unk) | M2 | M3 | View | Menu |

**Byte 21:**

| Bit | 7 | 6 | 5 | 4-0 |
|-----|---|---|---|-----|
| | MouseClick | ShowDesktop | AltTab | (unknown) |

### Touchpad (in XInput report)

| Bytes | Field | Format |
|-------|-------|--------|
| 26-27 | Touch X | U16 big-endian, 0-1024 |
| 28-29 | Touch Y | U16 big-endian, 0-1024 |

### IMU (Inertial Measurement Unit)

Each detachable controller has its own IMU.

**Low-resolution gyro:**

| Byte | Field |
|------|-------|
| 30 | Left Gyro X (U8) |
| 31 | Left Gyro Y (U8) |
| 32 | Right Gyro X (U8) |
| 33 | Right Gyro Y (U8) |

**High-resolution IMU (i16 big-endian):**

| Bytes | Field |
|-------|-------|
| 34 | Left IMU timestamp |
| 35-36 | Left Accel X |
| 37-38 | Left Accel Y |
| 39-40 | Left Accel Z |
| 41-42 | Left Gyro X |
| 43-44 | Left Gyro Y |
| 45-46 | Left Gyro Z |
| 47 | Right IMU timestamp |
| 48-49 | Right Accel X |
| 50-51 | Right Accel Y |
| 52-53 | Right Accel Z |
| 54-55 | Right Gyro X |
| 56-57 | Right Gyro Y |
| 58-59 | Right Gyro Z |

## Touchpad Report (Interface 0x01)

Report ID `0x01`, 20 bytes. Supports 3 simultaneous touch contacts.

| Bytes | Field |
|-------|-------|
| 0 | Report ID (0x01) |
| 1 | Flags: confidence_0, tip_switch_0, contact_id_0 |
| 2-3 | Touch X_0 (U16 LE, 0-1024) |
| 4-5 | Touch Y_0 (U16 LE, 0-1024) |
| 6 | Flags: confidence_1, tip_switch_1, contact_id_1 |
| 7-8 | Touch X_1 (U16 LE) |
| 9-10 | Touch Y_1 (U16 LE) |
| 11 | Flags: confidence_2, tip_switch_2, contact_id_2 |
| 12-13 | Touch X_2 (U16 LE) |
| 14-15 | Touch Y_2 (U16 LE) |
| 16-17 | Scan Time (U16 LE) |
| 18 | Contact Count |
| 19 | Button flags |

No explicit release event — release detected by ~4ms timeout with no report.

## Configuration Commands

Sent via HID feature reports. Examples:

| Command | Bytes | Description |
|---------|-------|-------------|
| SteamOS mode | `04 0a 01` | Switch to SteamOS-friendly mode |
| Polling 500Hz | `04 10 02` | Set polling rate |
| Gyro enable | `04 08 01` | Enable gyroscope |

## Linux Kernel Support

| Component | Status | What it does |
|-----------|--------|-------------|
| **xpad** | Upstream | Basic Xbox360 gamepad (sticks, buttons, triggers, rumble) |
| **hid-lenovo-go** | Targeting 7.1 | Hardware config only (RGB, DPI, calibration, sleep) |
| **Userspace (hhd/InputPlumber)** | Required | Gyro, back buttons, controller synthesis, DualSense emulation |

The kernel does not handle advanced controller synthesis — userspace daemons remain necessary for DualSense emulation, which is what this project does.

## Reference Projects

| Project | Language | URL |
|---------|----------|-----|
| hhd (Handheld Daemon) | Python | https://github.com/hhd-dev/hhd |
| InputPlumber | Rust | https://github.com/ShadowBlip/InputPlumber |
| ROGueENEMY | C | https://github.com/NeroReflex/ROGueENEMY |
| legion-go-tricks | Misc | https://github.com/aarron-lee/legion-go-tricks |
