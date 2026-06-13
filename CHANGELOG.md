# Changelog

All notable changes to the **CardPulse** project will be documented in this file.

---

## [1.0.0] — 2026-06-13

### Added
- **Initial Release** of CardPulse as a standalone credit card statement analyzer Android application.
- **Local PDF Parsing Engine**: Built using `pdfbox-android` to parse password-protected statement files without remote servers.
- **Bank Auto-Detection**: Core scanner that identifies statement headers to distinguish between **SBI Card, HDFC Bank, and Federal Bank** formats.
- **Pluggable Regex Parsers**: Bank-specific regex engines that parse transactions, dates (dd/MM/yyyy, dd-MM-yyyy, dd/MM/yy), Dr/Cr types, and handle multi-line narration continuation.
- **Keyword Auto-Categorizer**: Reusable category matcher that maps descriptions to 12 default categories and cleans merchant descriptions.
- **Interactive Dashboard**:
  - Combined total spent metrics.
  - Credit limit progress gauges.
  - Interactive, animated category donut chart.
  - Top 5 merchants breakdown.
  - Carousel card filters.
- **Activity & Search Screen**: Real-time transaction search, category chip filters, debit/credit tabs, and click-to-delete support.
- **Spending Trends (Analytics)**: Vertical bar charts showing 3-month and 6-month transaction trends with monthly average values.
- **Backup & Restore**: Secure export and restore of database data via JSON strings using local clipboard.
- **Privacy Enforcement**: Verified manifest excluding `android.permission.INTERNET` to guarantee offline data sandboxing.
