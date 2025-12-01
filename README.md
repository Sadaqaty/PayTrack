# 💸 PayTrack - Freelance Income Tracker

> **Get paid, stay organized, and never miss a payday!** 🚀

PayTrack is a modern Android application built for freelancers, contractors, and gig workers to effortlessly manage clients, track income, and generate professional invoices on the go.

---

## ✨ Features

*   **📊 Smart Dashboard:** Get a bird's-eye view of your financial health. Track monthly/yearly earnings, pending payments, and overdue invoices at a glance.
*   **👥 Client Management:** Manage client contracts, payment cycles (Hourly, Daily, Weekly, Monthly), and rates.
*   **📄 Professional Invoices:** Generate beautiful PDF invoices with a single tap. Customizable with your company details.
*   **🔔 Automated Reminders:** Never forget a due date! PayTrack notifies you when payments are due or overdue based on your contract cycles.
*   **💰 Multi-Currency Support:** Work with clients globally? No problem. Track payments in original currencies while viewing totals in your local currency with real-time conversion estimates.
*   **📉 Expense Tracking:** Keep tabs on your business expenses to see your true profit.
*   **💾 Data Freedom:** Full import and export functionality (CSV) to backup your data or analyze it in spreadsheets.
*   **🔒 Privacy First:** All data is stored locally on your device. No cloud servers, no subscriptions.

---

## 🛠️ Tech Stack

Built with love and **100% Kotlin** using the latest Android development standards:

*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 Design System)
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Local Storage:** [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) & [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (for payment monitoring notifications)
*   **Dependency Injection:** Manual / ViewModelFactory
*   **PDF Generation:** Native Android `PdfDocument` API

---

## 📸 Screenshots

| Dashboard | Client Details | Settings | Invoice PDF |
|:---:|:---:|:---:|:---:|
| <!-- Add screenshot here --> 📱 | <!-- Add screenshot here --> 👤 | <!-- Add screenshot here --> ⚙️ | <!-- Add screenshot here --> 📄 |

---

## 🚀 Getting Started

Want to build PayTrack yourself?

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/paytrack.git
    ```
2.  **Open in Android Studio:**
    Open the project folder in the latest version of Android Studio (Koala or later recommended).
3.  **Build & Run:**
    Connect your Android device or start an emulator and hit **Run** (▶️).

### Requirements
*   Android SDK 35
*   JDK 17+

---

## 🤝 Contributing

Found a bug? Want to add a cool feature? Contributions are welcome!
Feel free to open an issue or submit a pull request.

---

## 📜 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">
  <sub>Built with ❤️ by PayTrack Devs</sub>
</div>
