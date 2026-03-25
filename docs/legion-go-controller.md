# Lenovo Legion Go Controller

## Device Identification

- **VID**: `0x17EF` (Lenovo)
- **PID**: `0x6182` (XInput mode)
- **Hardware model**: 83E1

## USB Device Topology

A single USB composite device (`17EF:6182`) exposes 4 input interfaces through a USB hub:

```
USB Hub (usb-0000:c2:00.3-3)
└── 17EF:6182 "Legion Controller for Windows"
    ├── Interface 0 (1-3:1.0) ── xpad driver ──→ "Generic X-Box pad" (evdev + js)
    ├── Interface 1 (1-3:1.1) ── usbhid ──→ Touchpad (evdev + mouse)
    ├── Interface 2 (1-3:1.2) ── usbhid ──→ hidraw (vendor-specific XInput report)
    └── Interface 3 (1-3:1.3) ── usbhid ──→ Keyboard (evdev) + Mouse (evdev)
```

### What each interface provides

| Interface | Driver | Linux devices | Data |
|-----------|--------|---------------|------|
| 0 | xpad | evdev (gamepad), js | Standard Xbox360 buttons/axes, rumble (FF) |
| 1 | usbhid | evdev (touchpad), hidraw | Touchpad multi-touch (Report ID 0x01, 20 bytes) |
| 2 | usbhid | hidraw | **XInput report (Report ID 0x04, 64 bytes)** — ALL data in one packet |
| 3 | usbhid | evdev (keyboard + mouse) | Macro keys, mouse wheel, scroll |

### Which interface to use

**Interface 2 hidraw is the only one needed.** Its 64-byte XInput report contains everything:
- All standard buttons (A/B/X/Y, bumpers, triggers, sticks, D-pad)
- All extra buttons (Y1/Y2/Y3, M2/M3, Legion, QuickAccess, ShowDesktop, AltTab)
- Analog sticks and triggers
- Touchpad coordinates
- Dual IMU (accelerometer + gyroscope per controller)
- Battery status
- Controller dock/wireless state

Other interfaces are for the OS (cursor, keyboard shortcuts) and xpad (basic gamepad fallback). Projects like hhd and InputPlumber read exclusively from interface 2 hidraw.

### Identifying the correct hidraw device

Multiple hidraw devices are created (one per HID interface). To find the right one:
- Check that the first report has Report ID `0x04`
- Or filter by HID report descriptor size (interface 2 has rdesc_size=44)

## HID Interfaces (Report Formats)

The two relevant HID interfaces:

| Interface | Report ID | Size | Function |
|-----------|-----------|------|----------|
| 0x01 | `0x01` | 20 bytes | Touchpad (multi-touch, 3 contacts) — interface 1 |
| 0x02 | `0x04` | 64 bytes | XInput gamepad (all buttons, axes, IMU) — interface 2 |

The XInput report on interface 2 is the primary and only required data source.

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

## Configuration Protocol (Output Report ID 0x05)

Configuration commands are sent via HID output reports on the vendor-specific interface.
Format: `05 <len> <cmd> <param> ... 01`

The command byte is structured as two nibbles: `<mode><command>`, where mode `6` = XInput, `7` = FPS.

Source: [hhd-dev/hwinfo](https://github.com/hhd-dev/hwinfo/blob/master/devices/legion_go/peripherals/readme.md)

### Gyroscope

Enable/disable per controller:
```
0508 6a 02 <controller> <enable> 01
```
- Controller: `03` = left, `04` = right
- Enable: `00` = off, `01` = on

Remap gyro to joystick:
```
0508 6a 06 01 01 <gyro> <joystick> 01
```
- Gyro: `01` = left, `02` = right
- Joystick: `00` = disabled, `01` = left stick, `02` = right stick

### Touchpad

Enable/disable:
```
0506 6b 02 04 <enable> 01
```

Vibration on touch:
```
0506 6b 04 04 <enable> 01
```
- `01` = off, `02` = on

### Vibration Intensity

```
0506 67 02 <controller> <level> 01
```
- Level: `00` = off, `01` = weak, `02` = medium, `03` = strong

### RGB LED

Each controller has an RGB LED below the joystick. 3 profiles supported.

Toggle on/off:
```
0506 70 02 <controller> <on/off> 01
```

Select profile (1-3):
```
0506 73 02 <controller> <profile> 01
```

Set profile settings:
```
050c 72 01 <controller> <mode> <R> <G> <B> <brightness> <speed> <profile> 01
```
- Mode: `01` = solid, `02` = blinking, `03` = dynamic color
- Brightness/speed: `00`-`64` (0-100%)

### Stick Deadzones

```
0506 3f 06 <controller> <level> 01
```
- Controller: `03` = left, `04` = right
- Level: `00`-`63` (default `04` = 5%)

### Stick Sensitivity

```
0509 3f 02 <controller> <tx> <ty> <bx> <by> 01
```
Two-point curve: top point (tx, ty), bottom point (bx, by).

### Button Remapping (Back Buttons)

```
0507 6c 02 <controller> <button> <action> 01
```
- Buttons: `03 1c` = Y1, `03 1d` = Y2, `04 1e` = Y3, `04 21` = M2, `04 22` = M3
- Actions: `00` = disabled, `03` = LS click, `04`-`07` = LS directions, `08`-`0c` = RS, `0d`-`10` = D-pad, `12`-`15` = A/B/X/Y, `16`-`19` = bumpers/triggers, `23` = View, `24` = Menu

### Controller Sleep Timeout

```
0506 33 01 <controller> <minutes> 01
```

### Swap Legion Buttons with Start/Select

```
0506 69 04 01 <enable> 01
```
- `01` = normal, `02` = swapped

## Simple Configuration Commands

Sent as short HID commands (not Output Report 0x05):

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
| hhd-dev/hwinfo | Docs | https://github.com/hhd-dev/hwinfo |
| legion-go-tricks | Misc | https://github.com/aarron-lee/legion-go-tricks |
