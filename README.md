<h1 align="center"> Anchorage Shell </h1>
<p align="center">
  <b>A modular, native Android environment registry and execution layer</b><br/>
  Bypass Android's strict application sandboxes to create a shared, global ecosystem for mobile development toolchains.<br/>
  <i>Lightweight • Non-Root • Open-source</i>
</p>

<p align="center">
  <i>Anchorage stands for <b>Universal Environment Management</b></i><br/>
  This project solves the fragmentation of mobile development by establishing a centralized, encrypted <code>.env</code> registry and a universal <code>noexec</code> execution proxy. It allows mobile IDEs, compilers, and shell applications to share a single installation of development tools.<br/>
</p>

<p align="center">
  <i><b>The foundation for the upcoming Scriptle IDE</b></i><br/>
</p>

<p align="center">
  <a href="#platform"><img src="https://img.shields.io/badge/Platform-Android%2011%2B-3DDC84?logo=android" alt="Platform: Android 11+"/></a>
  <a href="#language"><img src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin" alt="Language: Kotlin"/></a>
  <a href="#language"><img src="https://img.shields.io/badge/Python-3.13%2B-3776AB?logo=python" alt="Language: Python 3.13+"/></a>
  <a href="#build"><img src="https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle" alt="Gradle 9.5.1"/></a>
  <a href="#license"><img src="https://img.shields.io/badge/License-MIT-yellow?logo=open-source-initiative" alt="MIT License"/></a>
</p>

---

<div align="center">
  <strong> Universal package discovery and environment routing made simple — one command away.</strong>
</div>

---

## The Vision & Problem Statement

Historically, Android has lacked a universal package discovery system. On Windows, developers rely on Environment Variables and PATH routing. On Linux, `/usr/bin` and package managers maintain a shared ecosystem. Android, however, isolates every application inside its own strict internal sandbox (`/data/data/package.name`).

This isolation results in duplicate installations, wasted storage, and a total lack of interoperability between mobile IDEs, compilers, and shells. Anchorage Shell was architected to bridge this gap without requiring root access.

By establishing a shared environment array on public storage and providing a native Android setup utility alongside a globally accessible Python CLI, Anchorage allows any terminal, compiler, or IDE to securely discover, reuse, and execute shared toolchains exactly once.

---
## Open the Manifesto PDF :

[Anchorage_Architecture_Manifesto.pdf](Anchorage_Architecture_Manifesto.pdf) ,
[About_Anchorage.pdf](About_Anchorage.pdf)

---

## Features & Capabilities

- **Zero-Root Ecosystem:** Operates entirely within Android's `MANAGE_EXTERNAL_STORAGE` bounds.
- **Dynamic Color-Blending UI:** A modern, programmatic Kotlin interface (No XML) with live Theme selection and Dark Mode integration.
- **Custom AES-ECB Cryptography:** A zero-dependency encryption layer perfectly synced between the Kotlin app and the Python CLI to protect system variables.
- **Universal Proxy Execution:** An advanced Shebang Interception Engine that completely bypasses Android's `noexec` kernel limits on public shared storage.
- **Package Management Wrapper:** Intelligently wraps native installation tools (like `pkg`) to install binaries and register them globally for external IDE consumption.

---

## Table of contents

- [Installation & Build Guide](#installation--build-guide)
- [System Architecture](#system-architecture)
- [CLI Reference](#cli-reference)
- [Understanding the Proxy Bypass](#understanding-the-proxy-bypass)
- [Future Goals](#future-goals)
- [About the Developer](#about-the-developer)

---

## Installation & Build Guide

### Prerequisites

To compile and link Anchorage natively on an Android device, you need a terminal environment (like Termux) with the following minimum toolchain installed:

- **Java JDK:** OpenJDK 21+ (`openjdk-21`)
- **Gradle:** 9.5.1+ (`gradle`)
- **Python:** 3.13+ (`python`)
- **Android SDK Tools:** `android-tools`, `aapt`, `aapt2`

### Phase 1: Compile the Android Bootstrap Application

The Anchorage Android app is responsible for establishing the physical storage architecture, granting the ecosystem raw filesystem clearance, and rendering the live encrypted `.env` variables.

1. Clone this repository to your local Android terminal environment.
   ```bash
   git clone https://github.com/Adi5423/AnchorageSetup.git
   cd AnchorageSetup
   ```
2. Execute the Gradle build wrapper natively:
   ```bash
   gradle clean assembleDebug
   
   ```
3. Copy the compiled APK out of the hidden build cache to your public storage:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk ~/storage/shared/Download/AnchorageSetup.apk
   
   ```
 4. Open your file manager, navigate to Downloads, and install AnchorageSetup.apk.

 5. Open the app, grant **All Files Access** when prompted, and tap **Initialize Core Layer**.
### Phase 2: Link the Universal CLI
The anchorage command-line utility acts as the ecosystem's brain, resolving paths, managing variables, and proxying tool execution.
1. Move the provided Python CLI script to your system's global binaries folder (e.g., inside Termux):
   ```bash
   cp cli/anchorage-cli.py $PREFIX/bin/anchorage
   
   ```
2. Make it globally executable:
   ```bash
   chmod +x $PREFIX/bin/anchorage
   
   ```
## System Architecture
Anchorage operates across four distinct technical layers:

| Layer | Name | Technical Function |
| :--- | :--- | :--- |
| **Layer 1** | Physical Storage | Located at /storage/emulated/0/. Hosts the hidden .anchorage/ directory (metadata) and .defaultPack/ directory (shared binaries). |
| **Layer 2** | Registry | Driven by a root-level anchorage.env encrypted file, alongside internal JSON schemas tracking installed packages. |
| **Layer 3** | Resolver Engine | The anchorage Python CLI. It parses the AES-encrypted registries, manages paths, and acts as the execution proxy. |
| **Layer 4** | App Integration | Future third-party applications (e.g., Scriptle IDE) that consume the Resolver to locate tools and run builds without duplicating logic. |

## CLI Reference
Once installed, the Anchorage CLI acts as both an environment manager and a transparent execution proxy.

### Environment Managemen
```bash
# View all active encrypted variables
anchorage showEnv
# View a specific mapped variable
anchorage viewEnv "DEFAULT_PACK"
# Register a new variable to the ecosystem
anchorage addEnv "PROJECT_ROOT" "/storage/emulated/0/Projects"
# Update an existing variable
anchorage upEnv "PROJECT_ROOT" "/storage/emulated/0/Projects/Scriptle"
# Safely delete a variable
anchorage deleteEnv "PROJECT_ROOT"
```
### Package Management
On dev:

Anchorage wraps native system package managers, ensuring tools are both installed locally and registered globally in the .anchorage/packages.json matrix for external IDEs to discover.
```bash
anchorage install <package>
anchorage remove <package>
anchorage list
```
## Understanding the Proxy Bypass
One of the largest technical hurdles was Android's Linux kernel security. Android mounts the public shared storage (/storage/emulated/0/) with the noexec flag. If a compiled binary or script is placed there, the kernel blocks execution with a Permission denied error.
Anchorage solves this using a **Universal Proxy Execution Layer**.
When you execute a command through Anchorage (e.g., anchorage python main.py or anchorage scriptle-test):
 1. It injects DEFAULT_PACK/bin into a temporary $PATH environment bubble.
 2. It locates the target file.
 3. If the file is on shared storage, Anchorage physically opens the file, reads the Shebang header (e.g., #!/bin/sh), and rewrites the command to pass the script directly to the interpreter (which lives safely in Termux's app data).
This allows seamless, global execution of scripts stored on public storage without requiring rooted devices.

---

## Future Goals
The current operational build successfully bridges the environment mapping and CLI integration. The immediate roadmap includes:
 * **Independent Package Extraction:** Upgrading the CLI from wrapping native pkg commands to utilizing curl and tar to directly download, compile, and route custom binaries from remote sources entirely into .defaultPack.
 * **SDK Generation:** Deploying Java, Kotlin, and Node.js SDKs so third-party mobile applications can query Anchorage without relying on the terminal execution layer.
 * **Project B (Scriptle IDE):** The development of the flagship, React-based IDE designed to sit directly on top of the Anchorage infrastructure.

---

### About the Developer

**Aditya Tiwari**
- 💼 LinkedIn: [Aditya Tiwari](https://www.linkedin.com/in/adii5423/)
- 🐱 GitHub: [@Adi5423](https://github.com/adi5423)
- 📧 Email: adii54ti23@gmail.com
- 🐦 Twitter: [@Adiii5423](https://twitter.com/Adiii5423)
- 📧 Instagram: [@Adii5423.exe](https://instagram.com/Adii5423_)

---

### Contributing
If you would like to contribute to this project, feel free to fork the repository and submit a pull request. Any contributions, suggestions, or improvements are welcome!

---
## License

This repository includes an **MIT License**. See [`LICENSE.txt`](LICENSE.txt) for details..

---
