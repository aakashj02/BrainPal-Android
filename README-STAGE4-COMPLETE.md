# Brain Pal — integrated tracking build

This update keeps the existing Room database and goal data and adds a platform breakdown, recent history, CSV export, a home-screen widget, goal milestone notifications, and tighter best-effort duplicate reduction.

## Open and run
1. Unzip the project.
2. Open the `BrainPalAndroid` folder in Android Studio.
3. If Android Studio asks for an SDK path, select your local Android SDK in Settings/Preferences → Android SDK. `local.properties` is intentionally not included because its path is machine-specific.
4. Wait for Gradle sync, then run `app` on your phone.
5. In Brain Pal → Home or Settings, tap **Enable tracking in Settings** / **Open Android accessibility settings**.
6. Enable **Brain Pal tracking**. Android displays an accessibility disclosure; review it and enable only if you consent.
7. Open Instagram Reels, Facebook Reels, or YouTube Shorts for testing. Return to Brain Pal to review the daily per-platform totals and History.
8. To add the widget: long-press the Android home screen → Widgets → Brain Pal → drag the Today widget onto the home screen.

## Included
- Per-platform totals for today and lifetime: Instagram, Facebook, YouTube.
- Home dashboard: today's total, yesterday, goal progress and goal streak.
- Daily goal persistence and CSV export of saved watch events.
- Recent event history with platform and timestamp.
- Accessibility-service permission status and explanation.
- Goal milestone notifications every 5 counted events beyond the daily goal (Android notification permission must be allowed).
- Home-screen widget showing today's count and goal.
- Local Room database; no sign-in, backend, or cloud sync.

## Important accuracy limits
Instagram, Facebook, and YouTube do not provide this app with an official API for the exact number of Shorts/Reels watched. The detector estimates from accessibility-visible text. Captions, languages, ads, app updates, missing captions, and interface changes can cause false positives or misses. Deduplication reduces repeated counts for stable visible text but cannot guarantee one-and-only-one count per real video. Check the History list and treat totals as estimates, not official platform analytics. No video pixels are recorded and no content is uploaded to a server.

## Build note
The source archive was updated here, but this environment could not download the Gradle 8.13 distribution because external network access was unavailable. Run **Build → Make Project** in your Android Studio environment and send any compile error text/screenshot if one appears. Keep a copy of your original project before replacing it.
