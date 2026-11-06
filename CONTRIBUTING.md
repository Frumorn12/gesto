# Contributing

Gesto is a Kotlin and Jetpack Compose app that controls a Linux desktop through [Gesto for Linux](https://github.com/Frumorn12/gesto-linux), which lives in its own repository.

## Development setup

You need JDK 17 or newer and the Android SDK (platform 36). The Gradle wrapper fetches the right Gradle.

```sh
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Code, comments and commit messages are in English. User-facing text goes in `app/src/main/res/values/strings.xml` with its Italian translation in `values-it/strings.xml`.

## Design direction

Short, plain sentences in the interface: no jargon (portal, fingerprint, backend, codec) on the main screens. Every gesture or command must also be reachable in a visible way. Light and dark themes both have to work. Do not copy proprietary branding, assets or trademarks.

## Engineering rules

- Do not store tokens in plain text.
- Do not bypass host key pinning.
- Keep protocol changes in step with Gesto for Linux and its `docs/PROTOCOL.md`. Wire identifiers that still say `waypad` (handshake labels, stream magic, the `waypad://` scheme) stay as they are, so older PCs and phones keep working.
- When the PC reports that something is not supported, say so clearly instead of failing silently.

## Testing

At minimum, run:

```sh
./gradlew testDebugUnitTest assembleDebug
```

Manual testing needs Gesto for Linux running on a PC: `gesto` shows its status and `gesto pair` prints a pairing code.
