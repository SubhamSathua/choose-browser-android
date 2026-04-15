# Changelog

## v2.1 — March 8, 2026

### Preview Page
- Fixed crash when opening the Find-in-Page dialog; replaced with a custom inline find bar (`<`, `>` navigation, match counter).
- JavaScript switch and toast messages renamed to "JavaScript"; terminology formalised throughout.
- Added Ad-Blocker mode selector (AdGuard DNS / Domain Filter List) under the JavaScript toggle.
- Added Dark Mode toggle (CSS injection, images preserved); replaced the Fire button in the More sheet with Dark Mode.
- Added Permissions management sheet (Camera, Microphone, Location, Storage) with runtime request and system-settings shortcut.
- Site favicon displayed live in the address bar.
- Keyboard now dismisses reliably when tapping outside the address bar.
- JavaScript switch state persists across sessions via SharedPreferences.
- Added tooltip ("Temp JavaScript toggle") on the address-bar JS button.
- Press-scale animation (0.8×) and white ripple on all buttons.
- Switches replaced with Material 3 `MaterialSwitch` styled with Windows Blue (#0078D4).

### App
- Added Privacy Policy screen (HTML/CSS, honours app theme via query parameter).
- Privacy Policy accessible from Settings → Help and from Preview Page → More.
