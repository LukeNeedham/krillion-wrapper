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
- Icon: `res/drawable/ic_launcher_foreground.xml` is a placeholder "K"; replace it with the game's logo
