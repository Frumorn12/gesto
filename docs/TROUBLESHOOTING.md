# Troubleshooting

Two places tell you most of what is wrong:

- **On the phone**: Settings › Diagnostics shows the connection, input, screen, audio and the devices attached to the phone.
- **On the PC**: `gesto doctor` checks the Wayland session, portals, screen capture, audio, input and network. The service log is `journalctl --user -u gesto -f`.

Gesto for Linux has its own guide for the PC side: [gesto-linux](https://github.com/Frumorn12/gesto-linux).

## Gesto does not find my PC

- Check that the service runs: `gesto` on the PC says whether it does, and prints its address. If it doesn't, `systemctl --user enable --now gesto`.
- Some routers block broadcast between Wi-Fi devices. In Add a PC, tap **Type the address** and enter the address `gesto` printed (port `47771`).
- A firewall on the PC must let in UDP `47770` (discovery) and TCP `47771` (everything else).

## Pairing

- **"This PC can't show the request. Use the code instead."** The notification server on the PC has no buttons. Run `gesto pair` on the PC and type the code, or scan its QR.
- **"Nobody answered on the PC."** The notification stays for 120 seconds. Try again: a new attempt replaces the old request.
- **"The code is wrong or expired."** Codes are single use and expire. Run `gesto pair` again.
- **"Too many attempts."** Pairing is rate limited. Wait a minute.
- **"This PC only accepts new phones on the home network."** You are on mobile data or another network. Pair at home, or see [Pairing over mobile data](#pairing-over-mobile-data).
- On a PC still running waypad-daemon 0.1 there is no notification: use the code from `waypad-daemon pair-code`, or update the PC to Gesto for Linux.

## "This PC's identity changed"

The app refuses to connect because the PC's host key is not the one it paired with. This is on purpose. On the PC, check whether its state was deleted or the key rotated (`gesto rotate-host-key`). If you know why it changed, forget the PC in Gesto (its menu › Forget) and add it again.

## "No longer paired"

The PC no longer knows this phone, usually because it was revoked with `gesto devices revoke`. Tap **Add again**.

## The touchpad does nothing

- If the touchpad says **"Approve on the PC"**, tap **Ask the PC** and approve the request that appears on the PC. It asks once.
- If it says **"This PC doesn't take control from the phone right now"**, the PC found no way to inject input. Run `gesto doctor`. On Hyprland, install and restart the portal:

  ```sh
  sudo pacman -S xdg-desktop-portal xdg-desktop-portal-hyprland
  systemctl --user restart xdg-desktop-portal xdg-desktop-portal-hyprland
  ```

  Then tap **Check again**.
- Diagnostics › Input shows which method the PC uses for pointer and keys.

## The screen

### "This PC can't share its screen"

Run `gesto doctor` and look at capture. On Arch, CachyOS and Hyprland the PC needs:

```sh
sudo pacman -S xdg-desktop-portal xdg-desktop-portal-hyprland pipewire wireplumber \
  gst-plugin-pipewire gst-plugins-good gst-plugins-bad gst-plugins-ugly
```

`gst-plugins-bad` brings `h264parse` and, on NVIDIA, the `nvh264enc` hardware encoder; `gst-plugins-ugly` brings the `x264enc` software encoder. With neither, the screen falls back to JPEG at roughly ten times the bandwidth.

### It stays on "Starting the screen…"

The PC is probably waiting for you to approve screen sharing, in a dialog that may be hidden behind other windows. Approve it once for good, on the PC:

```sh
gesto authorize-portal
```

### It is slow or blurry

- Turn on **Show fps and delay** in Settings: a small line over the screen shows the resolution, the fps and the delay.
- **Automatic** quality steps down when the network can't keep up, and says so. **Saver** (720p, 30 fps) is the lightest; **Smooth** is the one for games.
- In Settings › Advanced › Screen source, use a source under **Recommended** (hardware H.264, up to 60 fps). The **Fallbacks** are much slower: screenshots run at about 6 fps.
- Wi-Fi matters most: 5 GHz, close to the access point, or the PC on a cable.

### No sound

Check that **PC audio** is on in the quick menu (the purple button) and that the phone's media volume is up. Diagnostics › Audio shows whether the PC sends sound; `gesto doctor` checks audio capture on the PC.

### Taps land in the wrong place

Touches on the black bars around the picture are ignored on purpose. In **Touchpad** touch mode your finger moves the pointer and a tap clicks where the pointer is; switch to **Direct** in the quick menu to click where you touch.

## Keyboards, mice and controllers attached to the phone

- A keyboard or mouse attached to the phone is forwarded while you are on the Touchpad or the Screen. Diagnostics › External devices lists what the phone sees and what the PC accepts.
- A **controller** works on the Screen, in full screen or in game mode, so it can't wander around the phone's own interface. On the PC it becomes a virtual gamepad: `/dev/uinput` must exist and be writable by your user. `gesto doctor` tells you.

## Commands

- **Hold to suspend** stays off until `allow_suspend` is `true` in `~/.config/gesto/config.json` on the PC.
- **Apps on the PC** lists what you put under `apps` in the same file. Restart the service after editing: `systemctl --user restart gesto`. A misspelled program shows no error on the phone: try the command in a terminal first.
- Media buttons need a player that speaks MPRIS (`playerctl`); brightness needs `brightnessctl`.

## Wake does nothing

Wake-on-LAN only works with the PC on a wired network and wake-on-LAN enabled in its BIOS (and, on some cards, in Linux with `ethtool`). The phone learns the PC's network cards when it connects, so connect once while the PC is on.

## Pairing over mobile data

There is no relay: the phone has to reach the PC directly. Forward TCP `47771` to the PC, then on the PC:

```sh
gesto pair --qr --remote-address your-public-hostname.example --allow-public-pairing
```

The QR carries both the public and the home address, so the same code works on mobile data and on Wi-Fi. Without `--allow-public-pairing` (or `"allow_public_pairing": true` in the config), paired phones can reconnect from outside but new ones can't pair. The screen stream is not encrypted like the control channel, so over the internet prefer a VPN.

## The QR scanner does not open the camera

If you denied the camera, allow it in Android's settings for Gesto (Apps › Gesto › Permissions). The camera is only used to read the QR.

## Collecting logs

On the phone, with USB debugging:

```sh
adb logcat -v time | grep -E 'Gesto|ConnectException|SocketTimeout'
```

The tags are `GestoViewModel`, `GestoVideoStream`, `GestoVideoSession`, `GestoVideoRenderer`, `GestoAudioPlayer`, `GestoRemoteScreen`, `GestoTouchpad`, `GestoExternalInput` and `GestoQrScanner`. On the PC, `journalctl --user -u gesto -f` at the same time.

## The APK does not build

```sh
java -version                       # 17 or newer
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

The debug APK ends up in `app/build/outputs/apk/debug/app-debug.apk`. The SDK needs platform 36 (`sdkmanager "platforms;android-36"`).
