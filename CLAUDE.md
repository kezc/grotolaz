# Project: Holds

## Overview
This is a Kotlin Multiplatform web application using Compose Multiplatform, targeting both JavaScript (JS) and WebAssembly (Wasm) platforms. The project is set up for web development with Compose UI framework.

## Project Structure

```
holds/
├── composeApp/              # Main application module
│   ├── src/
│   │   ├── webMain/         # Web-specific code (shared between JS and Wasm)
│   │   │   ├── kotlin/com/wojtek/holds/
│   │   │   │   ├── App.kt           # Main Compose UI application
│   │   │   │   ├── Greeting.kt      # Greeting logic
│   │   │   │   ├── Platform.kt      # Platform abstraction
│   │   │   │   └── main.kt          # Application entry point
│   │   │   ├── composeResources/    # Compose resources (images, etc.)
│   │   │   └── resources/
│   │   │       ├── index.html       # Web page template
│   │   │       └── styles.css       # Custom styles
│   │   ├── jsMain/          # JavaScript-specific code
│   │   ├── wasmJsMain/      # WebAssembly-specific code
│   │   └── webTest/         # Web tests
│   └── build.gradle.kts     # App module build configuration
├── gradle/                  # Gradle wrapper and version catalog
│   ├── libs.versions.toml   # Dependency versions
│   └── wrapper/
├── build.gradle.kts         # Root build configuration
├── settings.gradle.kts      # Project settings
├── .gitignore
└── README.md
```

## Technology Stack

### Core Technologies
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.9.3
- **Compose Compiler**: 2.3.0
- **Targets**: JavaScript (browser) + WebAssembly (browser)

### Key Dependencies
- `androidx.lifecycle:lifecycle-viewmodel-compose`: 2.9.6
- `androidx.lifecycle:lifecycle-runtime-compose`: 2.9.6
- Compose runtime, foundation, material3, UI
- Compose resources and UI tooling preview

### Build System
- Gradle with Kotlin DSL
- Version catalog (libs.versions.toml)
- Multiplatform plugin configuration

## Development

### Build and Run Commands

#### WebAssembly (Wasm) Target (Recommended - Faster)
```bash
# macOS/Linux
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Windows
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

#### JavaScript (JS) Target (Broader Browser Support)
```bash
# macOS/Linux
./gradlew :composeApp:jsBrowserDevelopmentRun

# Windows
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

### Current Application State
The application currently contains:
- A simple UI with a button ("Click me!")
- Animated visibility toggle for content
- Display of Compose Multiplatform logo
- A greeting message from the `Greeting` class
- Material 3 theming with primary container background

## Package Structure
- **Root Package**: `com.wojtek.holds`
- **Main Components**:
  - `App.kt` - Main composable application
  - `Greeting.kt` - Platform greeting logic
  - `Platform.kt` - Platform abstraction layer
  - `main.kt` - Application entry point

## Git Status
Project is in initial setup phase with files staged for first commit. The `kotlin-js-store/` directory is untracked (build artifacts).

## Development Notes

### When Making Changes
1. Ensure all dependencies are installed before finalizing changes
2. Verify proper `.gitignore` is in place to avoid committing:
   - Build outputs
   - Dependencies
   - `kotlin-js-store/` directory

### Source Sets
- **webMain**: Shared web code (common for JS and Wasm)
- **jsMain**: JavaScript-specific platform code
- **wasmJsMain**: WebAssembly-specific platform code
- **webTest**: Web platform tests

### Testing
- Tests are located in `composeApp/src/webTest/`
- Uses `kotlin-test` library

## Resources
- [Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/)
- [Kotlin/Wasm](https://kotl.in/wasm/)
- Community: [#compose-web Slack channel](https://slack-chats.kotlinlang.org/c/compose-web)
- Issues: [YouTrack CMP Project](https://youtrack.jetbrains.com/newIssue?project=CMP)

## Next Steps / TODO
- Complete first commit
- Define application requirements
- Implement core functionality
- Set up CI/CD if needed
- Add comprehensive testing

---
*Last Updated: 2025-12-27*
