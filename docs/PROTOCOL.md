# Gesto Protocol Notes

The app implements the protocol documented by Gesto for Linux in [`docs/PROTOCOL.md`](https://github.com/Frumorn12/gesto-linux/blob/main/docs/PROTOCOL.md). These notes describe the client side.

Several wire identifiers still say `waypad` (the handshake labels, `WAYPAD_STREAM_V1`/`V2`, `waypad-control-port-stream-v2`, the `waypad://invite` scheme). They are part of the protocol, not product names, and stay as they are so that PCs and phones from before the rename keep working.

## Client behaviour

- Discovers PCs by UDP broadcast on port `47770`, sending both `GESTO_DISCOVER_V1` and the older `WAYPAD_DISCOVER_V1`, and accepting both the `gesto` and `dev.waypad.daemon` services.
- Connects to TCP port `47771` by default.
- Performs a P-256 ECDH key exchange with the PC.
- Verifies the PC's ECDSA signature over the handshake transcript.
- Computes and pins the PC's host key fingerprint.
- Encrypts every frame after the handshake with AES-GCM.
- Refuses a trusted PC whose fingerprint changed.
- Stores trusted PCs encrypted with an Android Keystore AES-GCM key.

## Pairing

The usual way is **approval**: the app sends an `approval_pair_request` with the phone's name and a 4-digit PIN it shows on screen. The PC shows a desktop notification with the same PIN and Allow and Deny buttons, and answers only when someone presses one of them. The request stays open for 120 seconds.

The other way is a **code**: someone at the PC runs

```sh
gesto pair
```

and types the one-time code on the phone (older PCs: `waypad-daemon pair-code`). The code travels inside the encrypted channel. Discovery data is never trusted as authority; it only gives an address and the expected fingerprint.

`gesto pair --qr` also prints an expiring invite as a QR code and as a link:

```text
gesto://invite?v=1&host=desktop&address=192.168.0.184&port=47771&fingerprint=aa%3Abb&code=123456&expires=1770000000&route=direct-lan
```

The app accepts `gesto://invite` and the older `waypad://invite`, from its QR scanner or a link, and asks for confirmation before pairing. Invites may include `address`, `lan_address` and `remote_address`: the app treats them as candidates, trying `remote_address` first and then the LAN addresses. An invite only bootstraps pairing; the app still verifies the signed handshake and pins the fingerprint.

## Commands

The app sends these command names:

```text
prepare_input
pointer_move
pointer_move_absolute
pointer_button
scroll
external_input
text
shortcut
media
volume
brightness
clipboard_set
system
launch_app
get_capabilities
list_screen_sources
start_screen_stream
stop_screen_stream
request_key_frame
set_desktop_audio_mute
```

Errors reported by the PC are turned into plain-language messages on the phone.

## External Android input

While a session is on the Touchpad or the Screen, keyboards, mice and controllers attached to the phone are forwarded with `external_input`:

```json
{
  "name": "external_input",
  "device_id": "android:7:abcd1234",
  "device_type": "mouse",
  "event": {
    "type": "pointer_move",
    "dx": 12.5,
    "dy": -3.0
  }
}
```

Event types:

```text
device_connected
device_disconnected
pointer_move
pointer_button
pointer_scroll
keyboard_key
controller_button
controller_axis
```

Keyboard events carry XKB keysyms and a pressed/released state. Controller axes are normalised to `[-1.0, 1.0]`, with Android's deadzones applied before transport. Under backlog, axis updates collapse to the latest value per axis, so stale stick positions are dropped instead of arriving in bursts. The PC's `external_input.controller` capability is authoritative; on Linux it is true when the daemon can create a `uinput` virtual gamepad.

## Screen stream

The app lists sources with `list_screen_sources` and starts the stream with `start_screen_stream`:

```json
{
  "name": "start_screen_stream",
  "source_id": "hyprland:monitor:DP-1",
  "max_fps": 60,
  "jpeg_quality": 58,
  "max_width": 1280,
  "max_height": 1280
}
```

`jpeg_quality` keeps its old name: for H.264 the PC maps it onto a bitrate. The PC answers with the session:

```json
{
  "session_id": "...",
  "stream_port": 47771,
  "token": "...",
  "codec": "h264",
  "transport": "waypad-control-port-stream-v2"
}
```

With `waypad-control-port-stream-v2` the app opens a second connection to the control port returned in `stream_port` and writes one JSON line, before any handshake:

```json
{"type":"stream_connect","token":"..."}
```

The PC attaches that socket to the pending session, writes `WAYPAD_STREAM_V2` (H.264) or `WAYPAD_STREAM_V1` (JPEG), then sends envelopes: `u32` header length, `u32` payload length (both big-endian), a JSON header and the payload. H.264 frames go to a hardware decoder on a `Surface`; JPEG frames to a software renderer. Desktop audio travels in the same envelopes with the `opus` codec. When the decoder needs a fresh start, the app sends `request_key_frame`.

The stream is protected by its one-time token but is not encrypted like the control channel.

Older daemons answered with `waypad-frame-stream-v1` and a dynamic port for each stream; the app still understands that shape.

Pointer input on the Screen uses `pointer_move_absolute` with coordinates local to the source. Touches are mapped through the contain-fit layout first, so the black bars around the picture are ignored.
