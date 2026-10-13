# Changelog

## Collapsible Advanced Settings & Home Page Menu Disabling
- Moved Allow JS, HTTP, and Download into a collapsible Advanced section with info buttons.
- Disabled page-specific actions (Open in, Home, Print, Find, Share, Dark mode, Open in app) on the home page.

## Responsive Multi-Screen Layouts & Watch Screen Optimization
- Added tailored responsive layouts for watches (sw200dp), small phones (sw280dp, sw320dp), and tablets (sw600dp).
- Optimized Home, Settings, About, Excluded Browsers, and Licenses screens with centered views and safe padding.

## Chrome-Style Context Menu & Strict Type Detection
- Added Chrome-style hold context menu for links, images, video, and audio with rich metadata and haptics.
- Fixed element type detection so image hold shows only image options and link hold shows only link options.

## Layout Architecture & Feature-Prefix Organization
- Reorganized all 36 layout files into feature-first prefix hierarchy (pp_, chooser_, download_, settings_, debug_).
- Synchronized all sw screen qualifiers and Java layout references with zero build regressions.

## Open in App Action in Preview More Menu
- Added 'Open in app' row right above Desktop Site with pp_open_in_apps icon.
- Dynamically resolves native handlers and hides automatically when no app links are available.

## Sample Deep Links Tester in Debug
- Added interactive test cards for Google Drive, Maps, YouTube Music, Gmail, and custom URLs.
- Verified zero Google Play policy impact with standard HTTPS App Links and safe intent testing.

## Supported Native Apps in Chooser Popup
- Dynamically queries supported native apps for the tapped link and lists them before browsers.
- Connected Preview page Open In menu to show unified chooser popup with supported native apps.

## Default Apps Links Inspector in Debug
- Added Default Apps Links screen in Debug to inspect supported domains and deep links per app.
- Implemented dual-pipeline link parser with optimistic real-time streaming and search filter.

## Download Popups Theming & 0.7 Opaque Dim Overlay
- Harmonized all download confirmation, sort, and file action popups to match blocked dialog styling.
- Added 70% opaque background dim overlay across all download-related dialogs and sheets.

## Alternative Apps Comparative Feature Analysis
- Completed deep scan of alternative browser routing projects and documented unique features in features.md.

## Downloads Page Popups & Sticky Header Refinement
- Replaced default system dialogs and menus with custom rounded dropdowns and themed confirmation popups.
- Added scroll-responsive sticky header elevation and updated back button to pp_back icon.

## Downloads Page Crash Fix
- Fixed ClassCastException crash when opening the downloads page by correcting view type.

## Native Downloads & Safety Protection
- Added download safety blocker and Opera-style confirmation sheet with harmful extension warnings.
- Added full Downloads manager page with sticky header, live search, categories, and multi-select actions.

## Allow HTTP Connection Toggle & Safety Blocking
- Added 'Allow HTTP connection' switch (disabled by default) in preview More menu with M3 monochrome styling.
- Blocked unencrypted HTTP traffic by default with safe error guidance and instant reload upon enabling.

## Connection Security Detection & Informative Top Sheet
- Implemented real-time SSL and HTTP security detection with dynamic lock and warning icons.
- Added tappable top sheet dialog explaining connection status and certificate details in plain English using Inter font.

## Address Bar Favicon Removal & Selectable Sheet Title
- Removed site favicon from address bar and changed cursor color to white.
- Made more menu title selectable with horizontal scroll indicator on overflow.

## Cross-Site Cookie Blocking & Responsive Sticky Settings
- Blocked cross-site cookies in Preview and unified exit toast on close and fire actions.
- Propagated sticky settings across sw screen qualifiers and added rounded card to About.

## Sticky Headers & Preview Downloads Action
- Implemented sticky headers across Settings and About screens similar to Debug page.
- Added Downloads grid action in Preview menu and relocated Permissions above Privacy Policy.

## Preview Page Icons & Interactive Address Actions
- Updated preview icons and added touch ripple, hover states, and press feedback to address bar actions.
- Fixed home page URL placeholder and resolved address bar copy/share touch handling.

## Error Page Suggestions & Typography Refinement
- Added contextual troubleshooting suggestions to the native error overlay.
- Replaced hardbound font references in the address bar with semantic typography.

## Monochrome Toggle Alignment
- Styled MaterialSwitch toggles to match high-contrast monochrome design for light and dark themes.
- Enforced dark-theme toggles in preview mode to align with dark preview appearance.

## Native Error Overlay & Unified Reload
- Replaced WebView error page navigation with a native Chrome-style overlay preserving target URL.
- Unified page reload across address bar, error overlay, and menu buttons.

## Exclusion Toggles & Asset Path Modernization
- Replaced browser exclusion eye icons with monochrome MaterialSwitch toggles.
- Relocated screenshots and branding assets to .github directory and updated docs.

## Material 3 Monochrome Theme — October 4, 2026
- Adopted official Material 3 Monochrome color scheme for app accents, buttons, and system bars.
- Harmonized high-contrast neutral tokens across light and dark theme configurations.

## Preview Page JS & Exit Updates
- Fixed temporary JavaScript lifecycle to follow persistent preference on load and reset on window close.
- Added "All data cleared" toast feedback when exiting Preview via close or fire buttons.

## Documentation & CI/CD — August 9, 2026
- Fixed workflow tag detection to run assembleAlpha for -alpha/-pre tags and upload alpha APKs.
- Updated alpha buildType in app/build.gradle to sign with release key on CI.

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
