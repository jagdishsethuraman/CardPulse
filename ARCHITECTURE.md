# CardPulse — Architecture

This document details the architectural decisions, data flow, and pattern implementations in CardPulse.

---

## High-Level Architecture

CardPulse uses an **offline-first MVVM (Model-View-ViewModel)** architecture with **Unidirectional Data Flow (UDF)**. It relies on manual dependency injection (no Hilt/Dagger) to keep execution fast, lightweight, and compile-time verifiable.

```
+-------------------------------------------------------------+
|                          UI Layer                           |
|      (Jetpack Compose Screens & Custom Canvas Widgets)      |
+------------------------------+------------------------------+
                               | Event (Action)
                               v
+-------------------------------------------------------------+
|                       ViewModel Layer                       |
|           (Exposes StateFlows for UI Consumption)           |
+------------------------------+------------------------------+
                               | Flow (State)
                               v
+-------------------------------------------------------------+
|                         Data Layer                          |
|             (Repository, Room DB, DataStore)                |
+-------------------------------------------------------------+
```

---

## Core Components

### 1. Data Layer

- **Room SQLite Database (`AppDatabase`)**: Exposes low-level tables for:
  - `CreditCard`: Name, bank, last 4 digits, limit, color hex.
  - `Statement`: Reference to card, start/end dates, total spent, source file.
  - `Transaction`: Reference to statement and category, date, description, amount, credit/debit flag, clean merchant name.
  - `Category`: pre-populated defaults containing category names, emojis, colors, and keywords.
- **DataStore (`UserPreferences`)**: Wraps Jetpack DataStore Preferences to manage user profile name, default cycle days, dark mode configurations, and onboarding flags.
- **Repository (`CardPulseRepository`)**: Aggregates Room DAOs into a clean API surface. It provides custom SQLite aggregates (e.g. `getCategoryTotals`, `getTopMerchants`) to calculate spending summaries directly inside SQLite for optimized memory and query speeds.

### 2. ViewModel Layer

ViewModels act as state machines that consume repository flows and transform database collections into clean UI states:
- **DashboardViewModel**: Computes statement-cycle summaries. If a specific card is selected, it aligns query timelines with that card's statements. If "All Cards" is selected, it falls back to calendar month groupings.
- **TransactionsViewModel**: Combines search inputs, category chip selections, and credit/debit tabs to emit a filtered stream of transaction records.
- **AnalyticsViewModel**: Generates monthly statistics (typically over the last 3 or 6 months) to feed the visual bar charts.
- **ImportViewModel**: Manages the transient state of statement imports. Scoped to the parent `MainActivity` (ComponentActivity) lifecycle so that parsed preview details are safely shared between the picker and category verification screen.

### 3. UI Layer (Compose)

- Built entirely using Jetpack Compose, Material 3, and NavDisplay (Navigation 3).
- **Custom Canvas Charts**:
  - `DonutChart`: Renders segment arcs based on category spend percentages, employing an animated sweep angle on launch.
  - `BarChart`: Renders vertical bars scaled to the maximum monthly spend of the timeline, formatting amounts into compact suffix styles (e.g. `₹1.5L` or `₹25K`).

---

## Statement Parsing Engine

The statement processing pipeline is designed to be pluggable and robust:

```
[PDF File Uri] 
       |
       v
[PdfExtractor] -------> Page Texts (List<String>)
       |
       v
[BankDetector] -------> BankType (SBI, HDFC, Federal)
       |
       v
[Sbi/Hdfc/Federal Parser] -> Raw Transactions (ParsedTransaction)
       |
       v
[CategoryMatcher] ----> Clean Merchant & Match Category via Keywords
       |
       v
[ImportPreview UI] ---> User Correction & Save
```

1. **PdfExtractor**: Initialized using `PDFBoxResourceLoader`. Reads stream data page-by-page. Handles password prompt decryption.
2. **BankDetector**: Scans the first page to identify string markers (e.g. "SBI Card", "HDFC Bank") to determine format layout.
3. **Parser**: Resolves transaction details using bank-specific regex patterns:
   - Matches multiline descriptions (continuation rows that don't start with a date).
   - Resolves statement period headers.
   - Cleans amount strings (commas, DR/CR suffixes).
4. **CategoryMatcher**: Auto-assigns categories by performing substring queries of category keywords against descriptions. Generates a readable merchant name by stripping location tags and common terminal prefixes (e.g. "AMAZON SELLER..." -> "Amazon").
