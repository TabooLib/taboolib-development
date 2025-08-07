# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ IDEA plugin for TabooLib development, providing project scaffolding and development tools for TabooLib-based Minecraft plugins. The plugin is built with Kotlin and targets IntelliJ IDEA 2024.3+ with Kotlin K2 compiler support.

## Build System

This project uses Gradle with the Kotlin DSL and requires Java 21:

- **Build**: `./gradlew build`
- **Build plugin**: `./gradlew buildPlugin` (produces distributable plugin in `build/distributions`)
- **Run IDE with plugin**: `./gradlew runIde`
- **Test plugin**: `./gradlew verifyPlugin`
- **Generate shadow JAR**: `./gradlew shadowJar`

The build system uses:
- `org.jetbrains.intellij.platform` plugin for IntelliJ development
- Shadow plugin for dependency bundling
- Kotlin 2.1.0 with K2 compiler support
- Target IntelliJ IDEA Community 2024.3.1

## Architecture

### Core Components

**Project Builder (`ProjectBuilder.kt`)**:
- Main entry point for the new project wizard
- Orchestrates the multi-step project creation process
- Integrates with IntelliJ's new project wizard framework

**Wizard Steps** (`step/` package):
- `BasicConfigurationStep`: Core project settings and module selection
- `ConfigurationPropertiesStep`: TabooLib-specific configuration  
- `OptionalPropertiesStep`: Additional project options

**Language Support** (`inlay/` package):
- TabooLib i18n language file folding and navigation
- Color highlighting for Minecraft color codes
- Line markers for language keys
- Editor notifications and gutter providers
- Settings management for folding behavior

**Code Intelligence**:
- `completion/`: Kotlin completion contributors for TabooLib APIs
- `inspection/`: Code inspections for TabooLib patterns
- `suppressor/`: Unused code suppressors for TabooLib annotations

**Utilities**:
- `Template.kt`: Handles project template downloading and extraction
- `Assets.kt`: Plugin icons and resources
- `Utils.kt`: Common utility functions

### Key Features

1. **Project Wizard**: Multi-step wizard for creating TabooLib projects with module selection
2. **i18n Support**: Advanced language file handling with folding, navigation, and color support
3. **Code Completion**: Intelligent completion for TabooLib APIs and patterns
4. **Code Inspections**: Static analysis for TabooLib-specific code patterns
5. **K2 Compatibility**: Full support for Kotlin K2 compiler mode

## Configuration

Key configuration files:
- `gradle.properties`: Version (1.41-SNAPSHOT), build settings, JVM options
- `libs.versions.toml`: Dependency versions (OkHttp 4.12.0, FreeMarker 2.3.32)
- Plugin configuration in `plugin.xml` with since-build: 243 (IntelliJ 2024.3)

## Development Notes

- Uses Kotlin 2.1 with language level 2.1, API level 1.9
- JVM target: Java 21
- Parallel builds and caching enabled for performance
- K2 mode support declared in plugin.xml
- Memory settings: 2GB heap, 512MB metaspace for builds
- Debug mode available via `-Ddebug` system property