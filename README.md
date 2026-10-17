# krillion-wrapper

A minimal Android app (`com.lukeneedham.krillionwrapper`) that wraps the web game
[Krillion](https://krillion.io/) in a full-screen `WebView`.

- Portrait only, system bars stay visible, screen kept on while playing
- Ads are blocked: requests to known ad hosts are dropped and banner containers are hidden
  (`AdBlocker.kt` holds the host list and the CSS selectors)
- Game saves (localStorage) and cookies persist between launches
- Links to other sites open in the system browser
- Back button walks the game's web history, then exits
- Retry screen when the game can't be reached
- Debug APKs are built on every PR by the shared [ci-workflows](https://github.com/LukeNeedham/ci-workflows);
  the APK link is posted as a comment on the PR

## Build

```
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
```

Needs JDK 17+ and the Android SDK (API 35).

## Changing it

- Game URL / host: `GAME_URL` and `GAME_HOST` in `MainActivity.kt`
- Orientation: `android:screenOrientation` in `AndroidManifest.xml`
- Icon: `res/drawable-nodpi/ic_launcher_foreground.png` (the Krillion shrimp, 432×432, kept inside the
  adaptive-icon safe zone) on the dark grey `icon_background` colour in `res/values/colors.xml`

## Debug signing

`app/debug.keystore` is committed on purpose and used to sign debug builds (standard Android debug
credentials, not a secret). CI runners otherwise generate a new random debug key every run, and
Android refuses to install a build over one signed with a different key. If you installed a build made
before this keystore was added, uninstall it once; later builds then update in place.
