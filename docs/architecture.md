# Architecture Documentation

## Overview

Sari-Sari Smart follows the **MVVM (Model-View-ViewModel)** architecture pattern with a **Repository** data access layer and **Room** database for persistence. The app uses **unidirectional data flow**: UI events flow down to the ViewModel, which updates the domain models, which are persisted through Room and observed back via StateFlow.

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        UI LAYER                                  │
│                                                                  │
│  ┌──────────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │   Compose        │    │  Components  │    │  Navigation   │  │
│  │   Screens        │───▶│(BottomSheet, │    │  (NavGraph)   │  │
│  │  (20 screens)    │    │ Tutorial,    │    │               │  │
│  │                  │    │ DevPanel)    │    │  Routes       │  │
│  └────────┬─────────┘    └──────────────┘    └───────┬───────┘  │
│           │                                          │          │
│           │  StateFlow.observeAsState()              │ navigate │
│           ▼                                          ▼          │
├──────────────────────────────────────────────────────────────────┤
│                      VIEWMODEL LAYER                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    AppViewModel                          │   │
│  │                                                          │   │
│  │  State (MutableStateFlow):                               │   │
│  │    _products, _dailyEntry, _specificSales               │   │
│  │    _debts, _endOfDayData, _restockTemp                  │   │
│  │                                                          │   │
│  │  Computed Properties:                                    │   │
│  │    todayRecordedSales, todayProfit, lowStockCount        │   │
│  │    totalOutstandingDebts, getBusinessTip()               │   │
│  │                                                          │   │
│  │  Actions:                                                │   │
│  │    startDay(), completeEndOfDay(), reopenClosing()       │   │
│  │    addSpecificSale(), recordDebtPayment()                │   │
│  │    generateTestSale(), importData(), resetAllData()      │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                       │
│                         │ viewModelScope.launch / collect()     │
│                         ▼                                       │
├──────────────────────────────────────────────────────────────────┤
│                      REPOSITORY LAYER                            │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    AppRepository                         │   │
│  │                                                          │   │
│  │  Wraps 6 DAOs: Product, DailyEntry, SpecificSale,        │   │
│  │   CustomerDebt, EndOfDay, RestockLog                     │   │
│  │                                                          │   │
│  │  Converts between: Room Entity ↔ Domain Model            │   │
│  │                                                          │   │
│  │  Returns Flow<List<DomainModel>> for reactive observation│   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                       │
│                         ▼                                       │
├──────────────────────────────────────────────────────────────────┤
│                       DATABASE LAYER                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    AppDatabase                           │   │
│  │                                                          │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │   │
│  │  │    DAOs     │  │   Entities   │  │  Converters    │  │   │
│  │  │             │  │              │  │                │  │   │
│  │  │ ProductDAO  │  │ ProductEntity│  │  Date ↔ Long   │  │   │
│  │  │ DailyEntry  │  │ DailyEntry   │  │  List ↔ JSON   │  │   │
│  │  │ SpecificSale│  │ SpecificSale │  │                │  │   │
│  │  │ CustomerDebt│  │ CustomerDebt │  │                │  │   │
│  │  │ EndOfDay    │  │ EndOfDay     │  │                │  │   │
│  │  │ RestockLog  │  │ RestockLog   │  │                │  │   │
│  │  └─────────────┘  └──────────────┘  └────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### Sale Recording Flow
```
User taps Save in SaleBottomSheet
        │
        ▼
SaleBottomSheet.onClick()
        │
        ▼
AppViewModel.addSpecificSale(sale)
AppViewModel.deductStock(product.id, qty)
  If customer: AppViewModel.addDebt() or addToDebtBalance()
        │
        ▼
MutableStateFlow updated (_specificSales, _products, _debts)
        │
        ├──▶ UI recomposes (DayModeScreen, MorningCheckScreen)
        │
        ▼
viewModelScope.launch { repository.saveSpecificSale(sale) }
        │
        ▼
SpecificSaleDao.insertSale(SpecificSaleEntity)
        │
        ▼
Room persists to SQLite
```

### Day Start Flow
```
User taps "Start the Day"
        │
        ▼
AppViewModel.archiveDaySales() (if different date)
AppViewModel.openDay()
   → dayOpen = true, dayDate = today, dayArchived = false
        │
        ▼
Navigate to DayModeScreen (Routes.DAY)
        │
        ▼
DayModeScreen observes:
  - _specificSales (today's filtered)
  - _dailyEntry
  - _debts
```

### EOD Editability Flow
```
User completes day → completeEndOfDay()
   → dayOpen = false, dayArchived = false
   → EndOfDayData saved with recordedSales, actualSales, profit

Next morning: button shows "Edit Today's Closing"
        │
        ▼
AppViewModel.reopenClosing()
   → Restores state from EndOfDayData
   → Navigate to EveningClosingScreen with ?edit=true
        │
        ▼
User edits values, taps "Update Closing"
   → completeEndOfDay() overwrites existing EndOfDayData
```

---

## Navigation Graph

```
SPLASH ──→ SETUP ──→ MORNING (tab 1)
                         │
                    ┌────┴────┐
                    │         │
                    ▼         ▼
                INVENTORY   DAY (tab 2, center FAB)
                    │         │
                    ▼         │
              ADD_STOCK      │
              PRODUCT_DETAIL  │
                    │         │
                    └────┬────┘
                         │
                         ▼
                    CLOSING (tab 3)
                         │
                    ┌────┴────┐
                    │         │
                    ▼         ▼
                DEBTS     MORNING (back to tab 1)
                    │
              ┌────┴────┐
              │         │
              ▼         ▼
          NEW_DEBT  CUSTOMER_DEBT_DETAIL
                         │
                         ▼
                   RECORD_PAYMENT

Support routes (accessible from headers):
  SETTINGS, HELP, RESTOCK
```

---

## State Management

### Reactive State
All ViewModel state is exposed as `StateFlow`:
- `products: StateFlow<List<Product>>`
- `dailyEntry: StateFlow<DailyEntry?>`
- `specificSales: StateFlow<List<SpecificSale>>`
- `debts: StateFlow<List<CustomerDebt>>`
- `endOfDayData: StateFlow<EndOfDayData?>`

### Auto-Save
Every state change is automatically persisted to Room via:
```kotlin
viewModelScope.launch {
    _products.collect { list ->
        list.forEach { repo.saveProduct(it) }
    }
}
```

### Initial Load
On app startup, `initRepository()` calls `doInitialLoad()` which uses `first()` on each Flow to get the latest persisted state. If no data exists, it calls `seedSampleData()`.

---

## Key Design Decisions

1. **Single Activity** — Navigation Compose handles all screen transitions within one activity
2. **Manual DI** — No Hilt/Dagger to keep complexity low; `SariSariApp` provides DB singleton
3. **Single ViewModel** — One `AppViewModel` shared across all screens for simplicity
4. **No Use Cases** — Business logic lives in ViewModel (sufficient for app complexity)
5. **StateFlow over LiveData** — Better support for coroutines and Compose integration
6. **Entity ↔ Domain separation** — Room entities are mapped to clean domain models
