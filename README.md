# PayTrack - Freelance Income Tracker

> **Get paid, stay organized, and never miss a payday!**

PayTrack is a modern Android application built for freelancers, contractors, and gig workers to effortlessly manage clients, track income, and generate professional invoices on the go.

---

## Features

- **Smart Dashboard:** Bird's-eye view of financial health. Track monthly/yearly earnings, pending payments, and overdue invoices at a glance.
- **Client Management:** Manage client contracts, payment cycles (Hourly, Daily, Weekly, Monthly), and rates.
- **Professional Invoices:** Generate beautiful PDF invoices with a single tap. Customizable with your company details.
- **Automated Reminders:** Never forget a due date! PayTrack notifies you when payments are due or overdue based on your contract cycles.
- **Multi-Currency Support:** Work with clients globally. Track payments in original currencies while viewing totals in your local currency with real-time conversion.
- **Expense Tracking:** Keep tabs on business expenses to see your true profit.
- **Data Freedom:** Full import and export functionality (CSV) to backup your data or analyze it in spreadsheets.
- **Privacy First:** All data is stored locally on your device. No cloud servers, no subscriptions.

---

## Tech Stack

- **Language:** 100% Kotlin
- **UI:** Jetpack Compose (Material 3 Design System)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Local Storage:** Room Database (SQLite) & DataStore
- **Concurrency:** Kotlin Coroutines & Flow
- **Background Tasks:** WorkManager (payment monitoring notifications)
- **Dependency Injection:** Manual / ViewModelFactory
- **PDF Generation:** Native Android `PdfDocument` API

---

## Screenshots

| Dashboard | Client | Wallet | Transactional Logs |
|:---:|:---:|:---:|:---:|
| ![Dashboard](https://github.com/user-attachments/assets/b59975df-5b2d-4189-842d-c8a146058863) | ![Client](https://github.com/user-attachments/assets/e55bb5ce-0b15-4a45-9b26-dd87d94b5e49) | ![Wallet](https://github.com/user-attachments/assets/6af095a7-8ed1-4240-8c68-76dca1f1bb7c) | ![Logs](https://github.com/user-attachments/assets/bb5bb492-1b26-4862-80e2-c5462f06f43c) |

---

## Getting Started

### Clone and Build

```bash
git clone https://github.com/Sadaqaty/PayTrack.git
cd PayTrack
```

### Local Build (Debug)

```bash
./gradlew assembleDebug
```

### Requirements

- Android SDK 35
- JDK 17+

---

## CI/CD

This project uses **GitHub Actions** for automated builds and releases.

### Workflow

1. Push to `master` triggers the CI/CD pipeline
2. The app is built with release signing
3. A signed APK is generated and attached to a GitHub Release
4. Version in `version.properties` is auto-incremented for the next build

### Version Management

The app version is managed in `version.properties` at the project root:

```properties
VERSION_CODE=1
VERSION_NAME=1.0.0
```

The CI workflow auto-increments both values after each release.

---

## Project Structure

```
app/
  src/main/java/com/fixare/studio/paytrack/
    data/           # Room database, DAOs, entities, repository
    ui/
      client/       # Client management screens
      dashboard/    # Dashboard screen
      log/          # Transaction logs screen
      settings/     # Settings screen
      wallet/       # Wallet/expense screen
      welcome/      # Onboarding screen
      components/   # Reusable UI components
      theme/        # Material 3 theme
    utils/          # CSV export, PDF generation
    worker/         # Background workers (notifications, currency sync)
```

---

## Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

---

## License

This project is available under the [MIT License](LICENSE).

---

<div align="center">
  <sub>Built by Fixare Studio</sub>
</div>
