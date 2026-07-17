# Sari-Sari Smart 📱

**A daily business companion for sari-sari store owners**

Sari-Sari Smart is a native Android application built with Kotlin and Jetpack Compose that helps traditional sari-sari (convenience) store owners track their daily sales, manage inventory, and monitor customer debts — all through a simple, mobile-first interface.

---

## ✨ Features

### Three Core Pillars

| Feature | Description |
|---------|-------------|
| **📦 Stock Tracker** | Color-coded inventory management with low-stock alerts, search, and quick-sell |
| **💰 Daily Cash Summary** | Record sales, track profit, and view real-time business insights |
| **📝 Utang Notebook** | Customer debt tracking with payment recording and full ledger history |

### Key Highlights

- **Three-Moment Navigation**: Morning Check → Day Mode → Evening Closing
- **Bilingual Support**: English and Filipino (Tagalog)
- **3 Text Sizes**: Standard, Large, Extra Large for accessibility
- **Tutorial System**: 14-step guided tour + page-specific tutorials
- **Restock Day**: 2-step physical count + purchase recording workflow
- **EOD Editability**: Re-open and edit closing data as needed
- **Developer Panel**: Hidden utility (double-tap Morning tab) for testing
- **Real-time Insights**: Business tips, sales trends, profit margin analysis
- **Weekly Snapshot**: 7-day sales totals and best-selling products

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 2.0.21 |
| **UI** | Jetpack Compose + Material Design 3 |
| **Architecture** | MVVM + Repository Pattern |
| **Navigation** | Navigation Compose (single-activity) |
| **Database** | Room (SQLite) with 6 entities |
| **State Management** | StateFlow + `collectAsStateWithLifecycle` |
| **DI** | Manual (no framework) |
| **Testing** | 48+ tests (DAO + ViewModel) |

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                       UI Layer                           │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  Screens    │  │  Components  │  │  Navigation   │  │
│  │(Composable) │  │(Reusable UI) │  │  (NavGraph)   │  │
│  └──────┬──────┘  └──────────────┘  └───────┬───────┘  │
│         │                                    │          │
│         └──────────┬─────────────────────────┘          │
│                    │ StateFlow / callbacks               │
├────────────────────┼─────────────────────────────────────┤
│              ViewModel Layer                             │
│    ┌──────────────────────────────────────┐             │
│    │         AppViewModel                  │             │
│    │  - Holds all app state               │             │
│    │  - Computed properties               │             │
│    │  - Business logic                    │             │
│    └──────────────────┬───────────────────┘             │
│                       │                                  │
├───────────────────────┼──────────────────────────────────┤
│              Repository Layer                            │
│    ┌──────────────────────────────────────┐             │
│    │         AppRepository                 │             │
│    │  - DAO ↔ Domain model conversion      │             │
│    └──────────────────┬───────────────────┘             │
│                       │                                  │
├───────────────────────┼──────────────────────────────────┤
│              Database Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Room DAOs   │  │  Entities    │  │  Converters  │  │
│  │  (6 DAOs)   │  │  (6 tables)  │  │  (TypeConv)  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Database Schema (6 Tables)

| Entity | Purpose |
|--------|---------|
| `ProductEntity` | Inventory items with price, quantity, threshold |
| `DailyEntryEntity` | Daily cash summary (expenses + earnings) |
| `SpecificSaleEntity` | Individual item sale records |
| `CustomerDebtEntity` | Customer debt tracking |
| `EndOfDayEntity` | Daily closing summaries |
| `RestockLogEntity` | Restock day history |

---

## 📱 Screens (20 total)

### Three Moments (Main Navigation)
| Screen | Description |
|--------|-------------|
| **Morning Check** | Stock warnings, debt summary, yesterday's recap, restock reminder |
| **Day Mode** | Live stats, transaction feed, sale bottom sheet, payment sheet |
| **Evening Closing** | Recorded/actual sales, profit, sold items, weekly snapshot |

### Support Screens
| Screen | Description |
|--------|-------------|
| Splash | Animated splash with auto-navigate |
| Setup | First-launch wizard (store name, owner, language) |
| Stocks | Product list with search, filters, status indicators |
| Add Stock | Add/edit product with markup helper |
| Product Detail | Item detail with edit/restock/deduct |
| Debts | Customer debt list with total summary |
| New Debt | Manual debt entry with autocomplete |
| Customer Debt Detail | Full customer ledger |
| Record Payment | Payment form with live preview |
| Restock Day | 2-step physical count + purchase recording |
| Settings | Language, text size, store info, data management |
| Help | Tutorial selector, how-to-use accordion |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 36
- Kotlin 2.0.21 plugin

### Setup
1. Clone the repository
2. Open the `Sari-sari_smart/` directory in Android Studio
3. Sync Gradle (File → Sync Project with Gradle Files)
4. Run on emulator or device (min SDK 24)

### Running Tests
```bash
# Run all DAO instrumented tests
./gradlew connectedCheck

# Run ViewModel unit tests
./gradlew testDebugUnitTest
```

---

## 🧪 Testing

| Test Type | Count | Location |
|-----------|-------|----------|
| Room DAO tests | 35 | `app/src/androidTest/.../dao/` |
| ViewModel unit tests | 13 | `app/src/test/.../AppViewModelTest.kt` |

---

## 📁 Project Structure

```
app/src/main/java/com/example/sari_sari_smart/
├── data/
│   ├── Models.kt                  # Domain models
│   ├── AppRepository.kt           # Data access layer
│   ├── SnackbarManager.kt         # Global snackbar system
│   ├── TimeUtils.kt               # Time formatting
│   └── local/
│       ├── AppDatabase.kt         # Room database singleton
│       ├── Converters.kt          # Type converters
│       ├── dao/                   # 6 Room DAOs
│       └── entity/                # 6 Room entities
├── ui/
│   ├── MainScaffold.kt            # Three-moment shell
│   ├── SupportScaffold.kt         # Sub-page scaffold
│   ├── localization/              # Strings (200+), AppSettings
│   ├── navigation/                # Routes, NavGraph, BottomNav
│   ├── theme/                     # Color, Theme, Typography
│   ├── components/                # Tutorial, SaleSheet, DeveloperPanel
│   └── screens/                   # 20 screens
├── MainActivity.kt                # Single activity
└── SariSariApp.kt                 # Application class
```

---

## 🔍 Developer Panel

Accessible via **double-tap the Morning tab** in the bottom navigation (within 600ms).

Includes:
- **Data Viewers**: View raw state (JSON), Export/Import data
- **Test Generators**: Generate test sales, debts, bulk add items
- **Restock Tools**: Clear restock data, set restock date, view log
- **Reset Actions**: Reset today, clear inventory, selective clear, full reset

---

## 📄 License

Capstone project for academic purposes.
