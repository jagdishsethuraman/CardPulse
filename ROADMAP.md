# CardPulse — Roadmap

This document outlines planned enhancements and milestones for the CardPulse project.

---

## Phase 1: Parsing Enhancements & Custom Rules

### Pluggable Bank Formats
- [ ] Add regex parser support for **ICICI Bank** credit card statements.
- [ ] Add regex parser support for **Axis Bank** credit card statements.
- [ ] Add regex parser support for **American Express (Amex) India** credit card statements.

### Custom Keyword Matching Rules
- [ ] Add a category keyword editor in **Settings** to let users add, edit, or delete custom keywords associated with each category.
- [ ] Allow users to override auto-categorization rules (e.g. mapping "Uber" to Business instead of Transport).

---

## Phase 2: Budgeting & Local Notifications

### Cycle Budgets
- [ ] Introduce spending budget limits per billing cycle.
- [ ] Show progress rings and alerts when card spending exceeds 50%, 80%, or 100% of the allocated cycle budget.

### Due Date Reminders
- [ ] Read the statement payment due date from PDF headers.
- [ ] Set up local system alarms (`AlarmManager`) to trigger local reminders 3 days and 1 day before the credit card payment is due.

---

## Phase 3: Password Vault & SMS Sync

### Local Password Manager
- [ ] Implement an encrypted password vault using Android **Keystore**.
- [ ] Let users save their statement PDF passwords locally so they don't have to enter them every time they import a new monthly statement.

### Offline SMS Sync (Optional)
- [ ] Add an optional permission to read transactional SMS alerts.
- [ ] Parse real-time spending messages locally to show real-time transactions before the monthly statement is generated.
