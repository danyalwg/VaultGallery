# Screen Reference Map

The eight local images are private layout references only. They are ignored by Git, must never enter APK resources, fixtures, documentation, or external services, and are identified below without reproducing personal content.

| Local reference | Product screen | Stable geometry to reproduce | Dynamic/private regions excluded from comparison |
|---|---|---|---|
| `pictures-timeline.jpg` | Pictures timeline | Upper-right actions, date heading, four-column edge grid, duration badges, fixed four-item navigation | All thumbnails, time/status icons, date/count text |
| `albums-main.jpg` | Albums landing | Action row, instructional card, Essential Albums title/action, three-column covers, labels/counts, fixed navigation | Covers, counts, status bar |
| `all-albums.jpg` | All Albums | Back/title/actions, three-column scrolling grid, rounded covers, two-line metadata | Covers, names, counts, status bar |
| `stories-menu.jpg` | Stories empty state with Menu panel | Upper-right actions, centered empty copy, bottom sheet size/radius, 4×2 action layout | Studio branding is omitted until implemented; status bar text |
| `gallery-settings-top.jpg` | Settings upper region | Back/title, grouped rounded cards, section labels, dividers, trailing switches | Provider-specific claim is replaced with a neutral disabled sync row |
| `gallery-settings-bottom.jpg` | Settings lower region | Album/location/privacy/about group geometry and switch states | Notification dots only appear from real app state |
| `album-selection.jpg` | Album multi-selection | Select-all/count/cancel header, album check circles, bottom actions, raised More menu | Covers, counts, legacy proprietary wording |
| `picture-selection.jpg` | Picture multi-selection | Select-all/count/cancel header, date-group checks, four-column thumbnails, bottom actions, right-side More menu | Thumbnails, place prompt contents, legacy proprietary wording |

## Legal substitutions

All icons will use original vector paths or permissively licensed Material Symbols. The reference handwritten font will not be copied; a documented OFL alternative with similar rounded metrics will be selected. Any reference action naming an operating-system secure container becomes `Copy to Secure Gallery` or `Move to Secure Gallery`. Provider, device, and vendor brands are not reproduced.

## Screenshot test masking

Tests compare system-inset boundaries, toolbars, grids, cards, dividers, menus, selection indicators, and navigation. Generated solid-color or abstract media replaces all personal thumbnails. System bars, timestamps, battery/network glyphs, dynamic counts, animation frames, and text whose value depends on fixtures are masked or asserted semantically.
