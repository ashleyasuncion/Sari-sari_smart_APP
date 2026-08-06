# Sari-Sari Smart — Tutorial Documentation (Android App)

This directory documents every tutorial in the Android application
(`git/app/Sari-sari_smart_APP/`), step by step. It serves as:

- A reference of the **current implementation** (single source of truth:
  `NavGraph.kt` + `Strings.kt` + `TutorialOverlay.kt`)
- A **QA testing checklist** for verifying tutorial behavior
- A guide for **future development / redesign**
- The **expected cross-platform behavior** (should match the web app's
  tutorials at `git/Sari-sari_smart/tutorials/`)

## Tutorial List

| File | Tutorial ID | Type | Steps | First Page |
|---|---|---|---|---|
| [main_tutorial.txt](main_tutorial.txt) | `main` | Multi-page (full flow) | 14 | morning |
| [home_tutorial.txt](home_tutorial.txt) | `home` | Page tutorial | 10 | morning |
| [stock_tutorial.txt](stock_tutorial.txt) | `stock` | Page tutorial | 10 | inventory |
| [sales_tutorial.txt](sales_tutorial.txt) | `sales` | Page tutorial | 10 | day |
| [debt_tutorial.txt](debt_tutorial.txt) | `debt` | Page tutorial | 10 | debts |
| [eod_tutorial.txt](eod_tutorial.txt) | `eod` | Page tutorial | 6 | closing |
| [report_tutorial.txt](report_tutorial.txt) | `report` | Page tutorial | 6 | morning |
| [settings_tutorial.txt](settings_tutorial.txt) | `settings` | Page tutorial | 5 | settings |
| [add_product_tutorial.txt](add_product_tutorial.txt) | `addProduct` | Page tutorial | 5 | add_stock |
| [new_sale_tutorial.txt](new_sale_tutorial.txt) | `newSale` | Page tutorial | 5 | day |
| [new_debt_tutorial.txt](new_debt_tutorial.txt) | `newDebt` | Page tutorial | 4 | new_debt |
| [help_tutorial.txt](help_tutorial.txt) | `help` | Page tutorial | 6 | help |
| [restock_tutorial.txt](restock_tutorial.txt) | `restock` | Page tutorial | 8 | restock |

## How to Read a Step Entry

Each step documents:

- **Step number** — 1-based position in the tutorial
- **Tutorial text** — the i18n key + English & Filipino copy (exception: the
  restock tutorial has no Filipino translation — it falls back to English)
- **Target page** — which screen/route the step renders on
- **Target UI element** — the Compose highlight target ID (or "none")
- **Highlighted component** — what the target points to
- **Tutorial box position** — computed at runtime (see rule below)
- **Highlight appearance** — white inner ring + green outer ring around the element
- **Expected page transition** — whether Next navigates to another route
- **Expected user interaction** — what the user does at this step
- **Navigation behavior** — Next / Finish / Skip / backdrop-dismiss
- **Animations / special behavior** — auto-scroll, fade, etc.

## Where the Config Lives

- **Main tutorial steps:** `NavGraph.kt` → top-level `tutorialSteps` (14 steps
  with `{ i18nKey, page, highlightTarget }`)
- **Page tutorials:** `ui/components/TutorialOverlay.kt` → `pageTutorials`
  (each has `id`, `labelKey`, `stepsKeyPrefix`, `stepCount`, `page`); step text
  is generated as `<prefix>1..<stepCount>`
- **Tutorial text (EN + FIL):** `ui/localization/Strings.kt` → the `en:` and
  `fil:` dictionaries (`tutorial1..14`, `homeTutorial1..10`, `stockTutorial*`,
  `salesTutorial*`, `debtTutorial*`, `eodTutorial*`, `reportTutorial*`,
  `settingsTutorial*`, `addProductTutorial*`, `newSaleTutorial*`,
  `newDebtTutorial*`, `helpTutorial*`, `restockTutorial*`)
- **Tutorial control:** `NavGraph.kt` → `startTutorial()`,
  `startPageTutorial()`, `advanceTutorial()`, `endTutorial()`
- **Overlay/highlight rendering:** `ui/components/TutorialOverlay.kt`
- **Highlight target registry:** `ui/components/TutorialHighlightState.kt`
  (`tutorialHighlight` modifier + `LocalTutorialHighlightState` +
  `LocalScreenScrollState`)

## Shared Runtime Rules (apply to every tutorial)

1. **Launch — auto:** the main tutorial auto-starts once per fresh app
   session when the Morning page first composes (`LaunchedEffect` on the
   MORNING route, guarded by `tutorialLaunchedThisSession`). First-ever launch
   (`launchCount == 1`) runs with `isReplay = false` (no Skip); later launches
   run with `isReplay = true` (Skip shown).
2. **Launch — manual:** from Settings → Tutorial selector
   (`startPageTutorial(id)`), from Help → "Replay Tutorial" (now routed through
   `startPageTutorial("main")`) and Help's tutorial selector, and from each
   page's header (?) button. Launching `main` manually behaves **identically**
   to the auto-launch (it uses the real 14-step `tutorialSteps` flow with page
   transitions and highlight frames — web v2.40 parity fix).
3. **Replay flag (`isReplay`):** first-ever launch → no Skip button and no
   backdrop dismiss; any replay → Skip button visible and backdrop tap dismisses.
4. **Box positioning rule** (`TutorialOverlay.kt`):
   - No highlight target → box at **bottom center**
   - Highlight target center in top 45% of overlay height → box at **bottom**
   - Otherwise → box at **top** (below the header pill)
5. **Highlight frame:** pure outline — transparent center so the element stays
   visible; a white inner ring (2dp) plus a green outer ring (4dp, offset 2dp
   outward), rounded corners (~12–14dp), offset −4dp around the registered
   bounds. Bounds come from `Modifier.tutorialHighlight(id, state)` registered
   via `onGloballyPositioned`; the frame follows the target's live bounds.
6. **Auto-scroll:** before showing a step with a highlight, the screen's
   `ScrollState` animates so the target is visible (160dp buffer below the
   header; scrolls up if the target would sit behind the header).
7. **Page transitions:** when the next step's `page` differs from the current
   route, `advanceTutorial()` navigates with
   `popUpTo(MORNING) { saveState = true }; launchSingleTop = true`. The
   DAY/CLOSING entry guards are skipped while the tutorial is active.
8. **Navigation buttons:** **Next** advances; on the last step the button
   becomes **Finish**; **Skip** appears only on replay; there is **no
   Previous**. Backdrop tap (replay only) dismisses in place.
9. **Completion:** `endTutorial()` hides the overlay and clears tutorial state.
   Completing the **main** tutorial also returns the user to the **Morning**
   page (web v2.40 parity). Page tutorials finish **in place**. Skip and
   backdrop-dismiss stay **in place** (no navigation).

## Known Platform Differences vs. the Web App

- **Page tutorials have no highlight frames on Android** (the page-tutorial
  generator produces steps with no `highlightTarget`), so the box sits at the
  bottom and the overlay does not move for `home`, `stock`, `sales`, `debt`,
  `eod`, `report`, `settings`, `addProduct`, `newSale`, `newDebt`, `help`,
  and `restock`. Only the **main** tutorial carries highlight frames.
- **Backdrop tap** dismisses the tutorial on Android (replay only); on the web
  the backdrop never dismisses.
- The web's page tutorials DO have highlight selectors; this is a known gap
  tracked for future Android work.

## Keeping These Docs Accurate

Whenever `tutorialSteps`, `pageTutorials`, or the i18n tutorial strings change,
update the matching `*_tutorial.txt` file. Each file's step list mirrors the
runtime steps 1:1 (main = `tutorialSteps`; page tutorials = `<prefix>1..N`).
