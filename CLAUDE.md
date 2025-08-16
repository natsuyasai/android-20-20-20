# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

- 必ず日本語で回答してください。
- ユーザーからの指示や仕様に疑問などがあれば作業を中断し、質問すること。
- コードエクセレンスの原則に基づき、テスト駆動開発を必須で実施すること。
- TDDおよびテスト駆動開発で実装する際は、すべてt-wadaの推奨する進め方に従ってください。
- リファクタリングはMartin Fowloerが推奨する進め方に従ってください。
- セキュリティルールに従うこと。
- 実装時は可能な限りテストコードも作成してください
- 実装時は適宜コミットを行ってください


## Project Overview

20-20-20 is a timer application implementing the 20-20-20 rule: 20-minute work periods followed by 20-second break notifications, designed to help reduce eye strain and maintain productivity.

**Core Functionality:**

- Configurable work (default 20 minutes) and break (default 20 seconds) intervals
- Optional repeat count or infinite loop mode
- Start, stop, pause/resume controls
- Background operation with persistent notification
- Customizable notification sounds and vibration settings

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Build APK
./gradlew assembleDebug
./gradlew assembleRelease

# Install to device/emulator
./gradlew installDebug

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture

**Technology Stack:**

- **UI:** Jetpack Compose with declarative UI patterns
- **State Management:** Compose State for reactive UI updates  
- **Development Approach:** Test-Driven Development (TDD) following t-wada methodologies
- **Code Quality:** Cyclomatic complexity kept under 10 where possible

**Project Structure:**

- **Package:** `com.example.a20_20_20`
- **Min SDK:** 28 (Android 9.0)
- **Target SDK:** 36
- **Compile SDK:** 36
- **Kotlin:** Version 2.0.21 with Compose plugin
- **Java:** Version 11 compatibility

**Key Dependencies:**

- Jetpack Compose BOM 2024.09.00
- Material3 for UI components
- Activity Compose for Compose integration
- Lifecycle Runtime KTX for lifecycle-aware components

## Development Guidelines

**Testing Strategy:**

- Unit tests in `src/test/` using JUnit 4
- Instrumented tests in `src/androidTest/` using Espresso and Compose testing
- Follow TDD principles for new feature development

**Background Processing Requirements:**

- Implement foreground service for timer continuation in background
- Notification channel setup for persistent timer display
- Handle system doze mode and battery optimization

**Audio/Vibration Features:**

- System notification sound selection
- Custom volume controls
- Vibration pattern configuration
- Handle audio focus management

**State Management:**

- Use Compose State for UI state
- Consider ViewModel for business logic and timer state
- Persist settings using DataStore or SharedPreferences