# AURA Music — Futuristic Upgrade Roadmap

Target branch: `aura-futuristic-upgrade` (based on the tested `aura-music-rebrand` branch)

## Preservation rules
- Keep the tested release branch intact and do not merge into `main` without user approval.
- Preserve current playback, library, downloads, playlists, login, and existing settings.
- Add features in small commits; run CI after each integration stage.

## Repository audit (initial)
- **Equalizer:** Existing EQ screen and ViewModel found (`ui/screens/equalizer`). Upgrade and polish this existing implementation instead of duplicating it.
- **Listening stats / Wrapped:** Wrapped screen, ViewModel, models and sharing helper already exist. Audit branding and improve the existing experience.
- **Widgets:** Several widget receivers and managers already exist. Improve/extend existing widgets rather than creating competing receivers.
- **Queue:** Queue/playback architecture needs a focused audit before changes.
- **Lyrics:** No lyrics implementation confirmed in this initial path-level audit; investigate playback/service and data sources before designing it.
- **Android Auto:** Verify MediaSession and automotive support before making changes; don't claim support until validated.

## Delivery phases
1. **Safe foundation & audit:** document feature integration points, check current player/service/MediaSession and dependency versions.
2. **Audio UI:** polish EQ presets/controls and add lightweight visualizer UI without continuous heavy work when playback is inactive.
3. **Lyrics:** integrate a reliable lyrics source and synchronized display; handle missing lyrics, timing and network errors.
4. **Queue intelligence:** smarter queue controls and recommendations, preserving manual queue ordering and playback state.
5. **AURA AI DJ:** mood/genre/activity prompt-based playlist or queue suggestions, using existing API infrastructure where feasible; gracefully degrade offline.
6. **Wrapped:** enhance existing listening summary with shareable AURA-branded story cards and privacy-conscious aggregation.
7. **Widgets & Android Auto:** audit existing widget implementations and MediaSession first, then improve and validate each separately.

## Validation
- CI build and unit tests for each stage.
- Manual device checks for playback, navigation, queue state, battery impact, permissions and offline behavior.
- Performance-sensitive animations should pause or reduce work when not visible.
- External/API-backed features must disclose limitations and avoid requiring paid keys without a fallback.

This file is a roadmap, not a claim that the feature set is already implemented.
