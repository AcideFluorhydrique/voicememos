# 🎙️ Voice Memos for Android

An Android voice recorder that looks and behaves like the iOS **Voice Memos** app — the
same one-screen list where a tap unfolds the player, the same red record dock, the same
yellow-handled trim editor — built on top of the excellent
[RecorderApp](https://github.com/tuuhin/RecorderApp) engine.

> Android has no good free and open source recorder with a proper editor. This fork keeps
> RecorderApp's recording, playback and editing engine untouched and replaces the Material
> interface with a Cupertino one.

## ✨ What it does

- **One screen, like iOS.** All recordings live in a single list under a large title with a
  search field. Tapping a row expands it into the player, tapping again folds it away.
- **Inline player.** Waveform you can scrub, 15 second skip buttons, playback speed
  (0.5× / 1× / 1.5× / 2×), share, delete and a trim shortcut, right inside the row.
- **Record dock.** The red button pinned to the bottom starts recording and lifts the
  recorder panel with a live waveform and a hundredths-of-a-second timer. Recording keeps
  going in a foreground service if you leave the panel, and the dock shows a live indicator.
- **Trim editor.** Drag the yellow handles over the waveform, then **Trim** to keep the
  selection or **Delete** to remove it. Edits are previewed on a scratch player and only
  written to disk when you save, always as a new recording.
- **Recently Deleted.** Deleted recordings rest for 30 days and can be recovered or purged.
- **Swipe actions, action sheets, alerts, haptics.** The small interactions that make an
  iOS app feel like one.
- **Localised.** English, 繁體中文 and 简体中文.

## 🧱 How it is put together

The upstream project is a clean multi module app, so nothing in the data layer had to be
rewritten. Two modules were added and the app now points at them:

| Module | What lives there |
| --- | --- |
| `core:cupertino` | The iOS design system: colour and type scales, canvas drawn SF-style glyphs, navigation bar, inset grouped lists, switch, segmented control, alerts, action sheets, swipeable rows and the three waveform views. No drawable resources, everything is drawn. |
| `feature:ios` | The screens: memos list with the inline player, recorder panel, trim editor, recently deleted and settings, plus the view models that drive them. |

Everything below the interface is upstream and unchanged:

- `data:recorder` — `MediaRecorder` inside a foreground service, amplitude stream, bookmarks
- `data:player` — Media3 `MediaSession` playback with a media notification
- `data:editor` — Media3 `Transformer` for cutting and cropping
- `data:visualizer` — `MediaCodec` PCM decoding for the waveforms
- `data:recordings` — MediaStore access, trash handling, metadata
- `data:datastore`, `data:database`, `data:categories`, `data:bookmarks`, `data:worker`

The Material screens that came with the upstream project have been removed, `feature:ios`
replaces all of them. `feature:widget` stays, it owns the home screen widgets and their
deep links now open the memos list.

## 🏗️ Building

The app builds on GitHub Actions, no local Android Studio needed:

- Every push to `main` or `dev` runs **Build APK** and uploads an installable debug APK as a
  workflow artifact (`voice-memos-debug-apk`), grab it from the
  [Actions tab](https://github.com/AcideFluorhydrique/voicememos/actions).
- Pushing a `v*` tag runs **Release APK** and attaches an unsigned release APK to the
  GitHub release.

Locally it is the usual:

```bash
./gradlew :app:assembleDebug
```

Requirements: JDK 17, Android SDK 36, minimum device API 29 (Android 10).

## 🔐 Permissions

| Permission | Why |
| --- | --- |
| Microphone | recording |
| Audio files | listing and playing your recordings |
| Notifications | the recording and playback notifications |
| Phone state *(optional)* | pause recording during a call |
| Location *(optional)* | tag recordings with a location, AAC and 3GP only |

## 🙏 Credits

Built on [RecorderApp](https://github.com/tuuhin/RecorderApp) by
[tuuhin](https://github.com/tuuhin), MIT licensed. This fork keeps the same licence. Voice
Memos is an Apple product and this app is not affiliated with or endorsed by Apple, it only
follows the interaction patterns people already know.
