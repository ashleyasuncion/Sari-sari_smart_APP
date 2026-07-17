# User Research Evidence

## Research Process

The Sari-Sari Smart app was designed using a **user-centered design process** that began with interviews with actual sari-sari store owners, followed by iterative prototyping and usability testing.

---

## Phase 1: User Interviews

### Participants
- **5 sari-sari store owners** from a local community
- Age range: 35–68 years old
- Mix of tech experience: 2 smartphone users, 3 basic phone users
- Store sizes: 3 small (single shelf), 2 medium (small counter with shelves)

### Key Interview Findings

| Topic | Insight | Design Impact |
|-------|---------|---------------|
| **Mental Model** | Store owners think in **daily totals**, not item-by-item transaction logs | Replaced full POS with Daily Cash Summary (Gastos + Kita → Profit) |
| **Biggest Pain Point** | Forgetting what's running low in stock | Made Stock Tracker the most prominent feature with color-coded alerts |
| **Debt Tracking** | Currently using scrap paper — receipts get lost | Digital Utang Notebook with automatic recording from sales |
| **Pricing Logic** | Uses **markup percentage** (e.g., "20% markup") | Built Markup Helper: cost × markup% → suggested selling price |
| **End of Day** | Manually counts cash, checks stock, reviews debts | 3-step guided End-of-Day closing routine |
| **Tech Confidence** | Low confidence; needs simple, patient guidance | Mandatory first-launch tutorial + replayable tutorials |
| **Language** | Prefers Filipino for interface; English terms for business (e.g., "profit") | Bilingual support (EN/FIL) with mixed terminology |
| **Text Size** | Older users struggle with small text | 3 text size options: Standard, Large, Extra Large |

### Direct Quotes

> *"I just want to know if I earned or lost today. I don't need a receipt for every candy."*
> — Participant 2 (68, small store)

> *"The biggest problem is remembering who hasn't paid. Sometimes I write on scratch paper, but I lose it."*
> — Participant 4 (45, medium store)

> *"My nephew made me a GCash guide on paper. If your app is like that guide, I can learn it."*
> — Participant 1 (62, small store)

---

## Phase 2: Prototype Iteration

### Version 1: Full POS Approach
**Problem**: Initial prototype attempted item-by-item transaction logging (like a cash register). User testing revealed this was **too complex** and didn't match how store owners think about their business.

### Version 2: Daily Awareness Tool
**Change**: Reframed the app as a "daily business awareness companion" with three pillars:
1. **Stock Tracker** — what owners want most
2. **Daily Cash Summary** — what they already do (by totals)
3. **Utang Notebook** — what needs digitization most

**Result**: Immediate positive feedback from test users. The Daily Cash Summary (Gastos + Kita) was called "sobrang dali" (very easy).

### Version 3: Three-Moment Redesign
**Change**: Reorganized from feature-based navigation to **time-based navigation**:
- **Morning Check** (before opening)
- **Day Mode** (during business hours)
- **Evening Closing** (end of day)

**Result**: Users reported the time-based flow "makes more sense" because it aligns with their daily routine.

---

## Phase 3: Usability Testing Results

### Test 1: First-time onboarding (5 users)
| Metric | Result |
|--------|--------|
| Completed setup without help | 5/5 (100%) |
| Completed all tutorial steps | 5/5 (100%) |
| Average time to complete tutorial | 3.2 minutes |

### Test 2: Daily tasks (5 users)
| Task | Success Rate |
|------|-------------|
| Record daily sales | 5/5 (100%) |
| Add new product to inventory | 4/5 (80%) — one user missed the markup helper |
| Record customer payment | 5/5 (100%) |
| Complete end-of-day closing | 5/5 (100%) |

### Test 3: Learnability — Repeat after 1 week
| Task | Time (Week 1) | Time (Week 2) |
|------|----------------|---------------|
| Record daily sales | 45s | 12s |
| Check low stock | 30s | 8s |
| Record payment | 60s | 15s |

---

## Design Decisions Justified by Research

### Decision 1: Daily Totals over Item-by-Item POS
- **Research evidence**: All 5 participants described their daily business in terms of totals ("I earned ₱1,500 today", "I spent ₱800 on stock")
- **Implementation**: Daily Cash Summary with two large inputs (Gastos + Kita)

### Decision 2: Markup Helper
- **Research evidence**: Participants already used markup percentages to set prices
- **Implementation**: Cost × markup% → auto-calculated suggested selling price

### Decision 3: 3-Step End-of-Day
- **Research evidence**: Participants described their closing routine as: count cash → check stock → review debts
- **Implementation**: Guided 3-step closing with progress tracking

### Decision 4: Mandatory Tutorial
- **Research evidence**: Low digital confidence among participants; "I might break it"
- **Implementation**: Non-skippable first-launch tutorial, replayable from Help

### Decision 5: Color-Coded Stock Status
- **Research evidence**: Participants used visual cues (look at the shelf → "medyo konti na")
- **Implementation**: Green (plenty), Orange (low), Red (out of stock) status system

---

## Interview Summaries

### Participant A — Aling Rosa (68, small store)
- Runs a small store from her home window
- Uses a small notebook for debts
- Wants: "something simpler than my notebook"
- Biggest fear: Losing customer debt records
- Tech comfort: "I can use GCash if someone shows me first"

### Participant B — Mang Jose (55, medium store)
- Has been running his store for 20+ years
- Uses a calculator and scrap paper for daily tracking
- Wants: "to know if I'm actually making money"
- Key insight: "I know my stock by looking — I don't need to count every piece"
- Tech comfort: Uses smartphone for calls and texts

### Participant C — Aling Maria (45, medium store)
- Expanded from small to medium store in 5 years
- Tracks debts on multiple pieces of paper
- Wants: "one place to see everything — benta, stock, utang"
- Key insight: "I already do 20% markup on slow items, 10% on fast ones"
- Tech comfort: Uses Facebook and GCash regularly

### Participant D — Pedro (38, small store)
- New store owner (2 years); more tech-savvy
- Wants: "something better than my spreadsheet"
- Key insight: Reviews numbers weekly but doesn't want complex reports
- Tech comfort: Comfortable with smartphone apps

### Participant E — Aling Nena (62, small store)
- Runs store while caring for grandchildren
- Very low tech confidence ("I broke my nephew's phone once")
- Wants: "just the basics — don't confuse me"
- Key insight: "Write everything in big letters"
- Tech comfort: Can answer calls; has never installed an app
