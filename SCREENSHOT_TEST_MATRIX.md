# Screenshot Test Matrix

All fixtures use generated abstract media and invented labels. Local references are never copied into test resources or golden outputs.

## Required screens

| Screen/state | Stable assertions |
|---|---|
| Pictures default / video badges | Insets, action alignment, date baseline, 4-column geometry, badge placement, bottom nav |
| Pictures selection | Header, group/item checks, fixed action bar, More anchor and clipping |
| Albums landing / All Albums | Card and section hierarchy, 3-column covers, truncation, empty favourite, navigation |
| Essential Album selection | Selected/unselected semantics, check geometry, action availability |
| Stories empty / Menu panel | Empty block placement, panel bounds/radius, 4×2 actions, attention state |
| Settings top/bottom | Section gaps, grouped cards, row heights, dividers, enabled/disabled switches |
| Photo viewer / Video player | Control placement, hidden-controls state, metadata panel, timeline |
| Secure login / empty / pictures / albums / selection / viewer / settings | Lock context indicator, no pre-auth content, secure protection state |
| Import progress/error | Byte/item progress, pause/cancel/retry, non-sensitive error |
| Secure recycle bin | Retention labels, selection, restore/delete confirmations |

## Devices and configuration

Run dark theme at the 996×2048 reference capture profile plus small phone, standard phone, large phone, landscape, tablet, and foldable configurations. Test font scales 1.0 and 1.3, LTR and a representative RTL locale, reduced motion, and high-contrast text where available.

## Comparison policy

Reference-profile stable edges target ±4 px, text baselines ±6 px, grid columns ±3 px, and radii ±2 dp. Mask system bars, generated thumbnail pixels, dynamic time/counts, cursor/focus blink, and animations. Pixel comparison is paired with semantic assertions for roles, labels, enabled/selected state, focus order, and minimum touch bounds. A golden update requires a reviewed reason and before/after artifact.

## Tooling decision

Phase 1 will evaluate a maintained Compose screenshot library against Android Gradle Plugin 8.8.2 and API 35. Tests must render deterministically in CI, support font scale and configuration changes, emit diffs, and keep private references outside test inputs. If the library cannot meet this, use emulator-driven Compose tests plus approved image comparison rather than an unstable dependency.
