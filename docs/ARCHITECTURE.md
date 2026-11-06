# Architecture

Gesto is a single-module Kotlin and Jetpack Compose app. It was called Waypad until 0.1; some storage names and wire identifiers still say `waypad` so that existing data and older PCs keep working.

## Layers

| Package | Responsibility |
| --- | --- |
| `core.network` | Discovery, handshake crypto, the encrypted channel, the command client (`GestoClient`), invites, wake-on-LAN and the screen stream client. |
| `core.pairing` | The Add a PC flow and the 4-digit PIN. |
| `core.session` | Which PC to reopen at launch, presence on the network, reconnect policy. |
| `core.input` | Touchpad and direct-touch gesture state machines, pointer acceleration, sticky modifiers, live text edits. |
| `core.externalinput` | Android `InputDevice`, `KeyEvent` and `MotionEvent` classification and normalisation for keyboards, mice and controllers attached to the phone. |
| `core.video` | H.264 hardware decoding onto a `Surface`, the legacy JPEG renderer, frame dropping, zoom, Automatic quality. |
| `core.audio` | Opus decoding and low-latency playback of the desktop's sound. |
| `core.screen` | Mapping phone touches to the PC's screen coordinates. |
| `core.storage` | Trusted hosts encrypted with the Android Keystore; settings in DataStore. |
| `core.errors` | Exceptions turned into plain-language messages. |
| `ui` | Compose theme, components, screens, the session scaffold (dock, quick menu, keyboard sheet). |
| root package | `MainActivity` and `GestoViewModel`. |

## Screens

`GestoViewModel` owns the current screen in its state; there is no `NavHost`. The back button is a pure function of the same state.

- **Launch**: reconnecting to the last PC, with a way to pick another.
- **Home** ("Your PCs"): paired PCs, presence, wake, rename, forget.
- **Add a PC**: network search, PIN approval, code, address, QR scan, invite confirmation.
- **Session**, with a dock at the bottom: **Touchpad**, **Screen**, the **Keyboard** sheet and **Commands**. The app reopens on the last mode used.
- **Settings**, **Advanced** and **Diagnostics**.

## Networking

The app keeps one long-lived TCP connection to the PC for control: no HTTP, no polling. Discovery uses UDP broadcast and is only a hint; an address typed by hand or carried by a QR invite works when broadcast is blocked.

The handshake is a P-256 key exchange signed by the PC's host key, followed by HKDF and AES-GCM frames. The host key is pinned at pairing.

Pairing has two paths. With **approval**, the phone shows a 4-digit PIN and the PC shows a desktop notification with the same PIN; the pairing completes when someone presses Allow on the PC. With a **code**, the user types the one-time code from `gesto pair`, or scans its QR invite (`gesto://invite`, the old `waypad://invite` still works). An invite can carry a LAN and a public address; the app tries them in order, and each one still goes through the signed handshake and pinning.

The **screen** is negotiated over the control channel, then a second TCP socket to the same control port carries the stream, after a one-line token request. Frames are length-prefixed envelopes: `WAYPAD_STREAM_V2` carries H.264, decoded by `MediaCodec` straight onto a `Surface`; `WAYPAD_STREAM_V1` carries JPEG for older PCs and fallback sources. The desktop's **sound** rides the same socket as Opus packets, played through `AudioTrack` in low-latency mode. Late frames and late audio are dropped rather than queued, because freshness matters more than completeness. The stream is protected by its token but is not encrypted like the control channel.

**Automatic quality** judges the frame rate actually shown in short windows and steps down one rung after several bad windows in a row. Smooth, Sharp and Saver are fixed presets; Advanced sets fps, size and encoder quality by hand (Custom).

**External devices** attached to the phone are normalised before they reach the network code: keys become XKB keysyms, mouse movement and wheel become pointer events, controller buttons and axes are normalised with deadzones. They go through the same backpressure-aware input queue as touch, and controller axis bursts collapse to the latest value. On the PC, the controller is a `uinput` virtual gamepad.

## Storage

Trusted hosts, tokens included, are serialised as JSON and encrypted with an AES-GCM key generated inside the Android Keystore before they reach `SharedPreferences` (`waypad_secure`, excluded from backups). Settings live in Preferences DataStore (`waypad_settings`) and are observed as `Flow`s.

## Wayland reality

The app does not pretend input is always available. It shows what the PC reports: if the PC has not approved control from the phone yet, the touchpad says so and offers to ask the PC; if the PC cannot share its screen, the Screen says so. Diagnostics shows the details, and `gesto doctor` on the PC explains them.

Touches on the screen are mapped through the contain-fit layout, so touches on the black bars are ignored instead of landing on the wrong spot of the desktop. The Screen follows the phone's rotation; in landscape it goes full screen and only the purple quick-menu button stays. Game mode sets the Smooth quality, full screen and controller forwarding together, and turning it off restores the previous quality.
