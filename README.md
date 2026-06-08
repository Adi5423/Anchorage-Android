# Anchorage Shell Ecosystem

Anchorage Shell is an open-source Android package ecosystem that provides a shared registry, workspace system, package store, and environment configuration layer. It allows mobile IDEs, compilers, and shells to share a single installation of development tools, bypassing standard Android application sandboxes without requiring root access.

For a deep dive into the system architecture, custom zero-dependency cryptography, and the `noexec` proxy bypass, please read the technical manifesto included in this repository: [Anchorage_Architecture_Manifesto.pdf](Anchorage_Architecture_Manifesto.pdf).

---

## Installation & Setup

To deploy the Anchorage Ecosystem on a new Android device, follow these two phases:

### Phase 1: Compile the Android Bootstrap Application
The Anchorage Android app is responsible for establishing the physical storage architecture and granting the ecosystem raw filesystem clearance.

1. Clone this repository to your local Android terminal environment (e.g., Termux).
2. Execute the Gradle build wrapper:
   ```bash
   gradle clean assembleDebug
   ```
