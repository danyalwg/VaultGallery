# Layout Measurements

## Calibration method

The references are 996 × 2048 pixels. Measurements below are inspection targets in reference pixels, followed by proposed responsive Compose values. Physical pixel density is not encoded reliably in the files, so implementation will use a 411 dp-wide reference profile (about 2.423 reference px/dp) and screenshot calibration; no pixel value is hardcoded in production.

Common system insets are approximately 60 px top (25 dp) and 104 px bottom (43 dp). Main content spans x=24..972 px (about 10..401 dp). Primary touch targets remain at least 48 dp even when visible glyphs are 24–28 dp.

| Screen | Insets and content boundary | Toolbar/navigation | Margins, grid, cards | Text/icons and states | Scroll/action behavior |
|---|---|---|---|---|---|
| Pictures | Top 60 px; bottom system 104 px; media x=24..972 px, y≈365..1785 px | Actions centered near y=188 px; bottom nav y≈1787..1944 px | 24 px horizontal margin; 4 columns; 3–4 px gaps; square ≈234 px cells; no thumbnail radius | Date heading ≈30 px; action glyphs ≈43 px; video pill lower-left; selected nav near-white, others grey; orange dot on Menu | Timeline scrolls behind fixed action space and stops above fixed nav; tap opens viewer, long-press selects |
| Albums | Top 60 px; content x=24..972 px; bottom nav starts ≈1785 px | Add/search/overflow near y=185 px; fixed nav | Instruction card x=24..972, y≈266..553, radius≈56 px; grid x≈24/347/671; cover≈300×300 px; gap≈23 px; cover radius≈46 px | Section title ≈43 px; album title ≈35 px; count ≈24 px; action blue; empty cover disabled grey | Vertical grid scrolls under fixed nav; card dismiss/settings actions; View all navigates |
| All Albums | Top 60 px; content x=24..972; bottom inset 104 px | Back/title baseline≈198 px; actions right | 3 columns; x≈24/347/671; covers≈300 px square; row pitch≈424 px; radius≈48 px | Title ≈46 px; album ≈34 px; count ≈24 px; long names ellipsized; enabled icons white | Entire album list scrolls; back returns; add/search/overflow act on albums |
| Stories empty + Menu | Top 60 px; full black content; system bottom 104 px | Search/overflow at y≈185 px; underlying nav fixed | Empty block centered around y≈975 px; menu x=24..972, y≈1238..1890, radius≈58 px; 4 equal columns × 2 rows; optional full-width row omitted | Empty title ≈36 px and body ≈30 px muted; menu glyph≈46 px; settings attention dot orange | Stories remains inert empty state; Menu opens modal rounded panel, outside tap/back closes |
| Settings top | Top 60 px; content x=24..972; system bottom 104 px | Back/title baseline≈198 px; no bottom app nav | First card y≈265..490; groups separated by ≈40 px; card radius≈56 px; row heights≈133 px; inner x≈63..934 | Title≈46 px; row≈40 px; support≈29 px; 1 px dividers; blue on, grey off; disabled copy muted | Whole page scrolls; rows navigate or toggle; disabled integrations explain status without action |
| Settings bottom | Top 60 px; content x=24..972; system bottom 104 px | Same back/title | Albums card y≈573..1148 with three ≈190 px rows; location card y≈1200..1333; privacy y≈1427..1699; about y≈1748..1881; radius≈55 px | Same hierarchy; orange state dot only if actionable; trailing switches≈85×50 px | Continuous settings scroll; policy/about open documents; permission opens app-specific guidance/system settings |
| Album selection | Top 60 px; content x=24..972; action bar y≈1795..1940 | Header select-all x≈82, count baseline≈200, cancel right; fixed bottom actions | 3 columns with ≈300 px covers and ≈23 px gaps; 52 px selection circles inset≈32 px; More menu x≈251..947, y≈1480..1780, radius≈52 px | Selected circle filled near-white with dark check; unselected transparent with white 3 px stroke; disabled `View all` muted | Selection survives rotation; back/cancel exits; bottom actions filtered; menu anchors above More and dismisses outside |
| Picture selection | Top 60 px; content x=24..972; action bar y≈1783..1945 | Same header; fixed bottom Create/Share/Delete/More | 4 columns with ≈234 px squares, 3–4 px gaps; date selector x≈76; item checks ≈47 px; menu x≈405..947, y≈571..1777, radius≈54 px | Count≈45 px; date≈31 px; menu rows≈112 px; selected circles filled/check, unselected outlined; destructive action labelled | Grid scrolls behind anchored menu; date check selects group; invalid actions hidden; Android confirmation precedes mutation |

## Responsive constraints

- Timeline columns: 3/4/5 according to saved density on compact width; clamp each cell to 72–160 dp and add columns on larger windows.
- Album grid: minimum cell width 120 dp, 10 dp outer padding, 10 dp inter-column spacing, square covers, 20 dp radius.
- Cards: 10 dp outer margin, 24 dp radius, 16–24 dp internal padding; settings row minimum 64 dp and grows with font scale.
- Bottom navigation: 80 dp content plus navigation-bar inset on compact screens; switch to an 80 dp rail at expanded width.
- Menus use available height, never cover system navigation, and become scrollable at font scale 1.3 or landscape.
