# Design Tokens

## Color roles

| Token | Initial value | Use |
|---|---:|---|
| `Background` | `#000000` | Edge-to-edge root |
| `SurfaceGrouped` | `#1B1B1E` | Settings and instruction cards |
| `SurfaceRaised` | `#29292D` | Menus and modal sheets |
| `SurfacePressed` | `#34343A` | Press/selected feedback |
| `TextPrimary` | `#F7F7FA` | Titles and primary labels |
| `TextSecondary` | `#9A9AA2` | Counts, support, disabled labels |
| `ActionBlue` | `#3D82F6` | Enabled switches and text actions |
| `AttentionOrange` | `#F36A1D` | Non-color-only attention companion dot |
| `Divider` | `#38383D` | Group separators |
| `Scrim` | `#99000000` | Modal background |
| `SecureAccent` | `#7EC8B2` | Original secure-context accent, paired with lock label/icon |
| `Error` | `#FF6B6B` | Failures and destructive emphasis |

Final values are calibrated in Phase 1 screenshot tests and checked for WCAG contrast. Selection and error states always include icon/text, not color alone.

## Dimensions

- Spacing scale: 2, 4, 8, 10, 12, 16, 20, 24, 32, 40, 48 dp.
- Grid gap: 1.5 dp timeline; 10 dp albums.
- Card/raised menu radius: 24 dp; album thumbnail radius: 20 dp.
- Visible icons: 24 dp standard, 28 dp primary; touch target: 48 dp minimum.
- Compact toolbar content: 64 dp plus status inset.
- Compact bottom navigation: 80 dp plus navigation inset.
- Divider: 0.5 dp visual, pixel-aligned.
- Motion: 100 ms press, 180 ms state, 240 ms panel; reduced motion removes spatial transitions.
- Elevation: 0 dp base/grouped, 6 dp raised menu with tonal separation retained in dark mode.

## Typography

Phase 1 will select an OFL-licensed rounded handwritten alternative after screenshot metric testing; the application must include its licence. Until then, the fallback is system sans-serif rounded. Proposed roles: screen title 28sp/34, section 24sp/30, card title 20sp/26, body 17sp/24, support 14sp/20, album title 18sp/24, album count 13sp/18, nav 14sp/18, dialog action 18sp/24, duration 12sp/16, selection count 28sp/34, empty title 22sp/28, empty body 16sp/24. Font scale is never clamped.

## Shapes and feedback

Cards and sheets use continuous rounded rectangles. Album covers are rounded; timeline media remains square and tightly tiled. Press feedback uses bounded tonal indication. Switches use native semantics with custom colors. Focus rings are 2 dp and keyboard-visible. Haptics are reserved for selection entry, destructive confirmation, and secure lock events where device settings allow.
