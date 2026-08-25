# My Car Companion

**My Car Companion** is a comprehensive Android application designed to help vehicle owners track maintenance based on mileage. It provides a specialized experience for managing multiple vehicles, automated mileage tracking, and predictive maintenance insights.

## 🚀 Project Overview

Maintaining a vehicle can be complex. **My Car Companion** simplifies this by monitoring your vehicle's health through a tiered mileage engine and proactive notifications. Built with modern Android technologies, it offers a seamless experience across all form factors.

## ✨ Core Features

*   **Garage & Vehicle Management:** Support for multiple vehicles with detailed specifications.
*   **Tiered Mileage Engine:** 
    *   **Android Auto Integration:** Automated tracking when connected to your car.
    *   **GPS Foreground Service:** Precise tracking for active trips.
    *   **Manual Entry:** Simple input for legacy tracking or corrections.
*   **MVI-driven Predictive Maintenance Dashboard:** A data-driven view of upcoming service needs based on historical usage.
*   **Proactive Notifications:** Powered by `WorkManager` to remind you of critical maintenance before it's too late.
*   **Adaptive UI:** Fully responsive design supporting Phones, Tablets, and Foldables using Material 3 Adaptive.
*   **Multi-unit Support:** Seamlessly switch between Kilometers and Miles.

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3 Expressive)
*   **Dependency Injection:** Hilt
*   **Local Database:** Room
*   **Background Tasks:** WorkManager
*   **Navigation:** Jetpack Navigation 3
*   **Adaptive Layouts:** Material 3 Adaptive Library
*   **Concurrency:** Kotlin Coroutines & Flow

## 🏗️ Architecture

The project follows **Clean Architecture** principles combined with the **MVI (Model-View-Intent)** pattern for the presentation layer. This ensures a unidirectional data flow, making the app predictable, testable, and maintainable.

## 📦 Modularization Details

The project is highly modularized to improve build times and separation of concerns:

*   **`:app`**: The main entry point, handling application-wide configuration, DI setup, and root navigation.
*   **`:core:model`**: Pure Kotlin module containing domain entities used across the entire project.
*   **`:core:database`**: Local data persistence using Room, including DAOs and Entity definitions.
*   **`:core:data`**: Implementation of repositories, managing data flow between the database and features.
*   **`:core:designsystem`**: Shared UI components, theme definitions (Material 3), and design tokens.
*   **`:core:notifications`**: Centralized logic for scheduling and displaying maintenance alerts.
*   **`:feature:home`**: The predictive maintenance dashboard and main landing experience.
*   **`:feature:vehicles`**: Garage management, allowing users to add, edit, and switch between vehicles.
*   **`:feature:parts`**: Management of specific vehicle parts and their individual maintenance schedules.
*   **`:feature:tracking`**: The core mileage engine, handling GPS services and Android Auto integration.

## 🛠️ Setup & Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-repo/my-car-companion.git
    ```
2.  **Open in Android Studio:** 
    Use the latest version of Android Studio (Ladybug or newer).
3.  **Sync Gradle:** 
    Wait for the project to sync and download all dependencies.
4.  **Run:** 
    Select the `app` configuration and click **Run**.

## 📈 Project Status

This project is currently in the **MVP (Minimum Viable Product)** stage. We are actively refining the mileage prediction algorithms and expanding Android Auto capabilities.

---
*Developed with ❤️ for car enthusiasts.*
