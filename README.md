![](.github/media/SM.svg)

![Build](https://github.com/sourceplusplus/SourceMarker/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

![](.github/media/portal_overview.png)

## Description

<!-- Plugin description -->
SourceMarker is a JetBrains-based plugin which implements Feedback-Driven Development (continuous feedback) technology via integration with [Apache SkyWalking](https://github.com/apache/skywalking). SourceMarker can be used to help debug and prevent production runtime issues by providing contextualized behavior about your source code throughout development.
<!-- Plugin description end -->

## Features

- Source code contextual user interface
- Service, service instance, endpoint metrics
- Database access metrics
- Integrated distributed trace mapping
- Performance anti-pattern detection
  - Performance Ramp

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "SourceMarker"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/sourceplusplus/SourceMarker/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Getting Started

todo

# Development

## Project Structure

### Framework

| Module                        | Description                                                          | Version |
| ----------------------------- | -------------------------------------------------------------------- | ------- |
| :mapper                       | Tracks source code artifact refactoring                              | 0.0.1   |
| :marker                       | Used to tie visual marks & popups to source code artifacts           | 0.0.1   |
| :mentor                       | Produces source code artifact informative/cautionary advice          | 0.0.1   |
| :portal                       | Used to visually display contextualized artifact data/advice         | 0.0.1   |
| :protocol                     | Common communication data models                                     | 0.0.1   |

### Implementation

| Module                        | Description                                                          | Version |
| ----------------------------- | -------------------------------------------------------------------- | ------- |
| :monitor:skywalking           | Apache SkyWalking monitor implementation                             | 0.0.1   |
| :plugin:jetbrains             | JetBrains plugin implementation                                      | 0.0.1   |

## Attribution

This project was highly influenced by [PerformanceHat](https://github.com/sealuzh/PerformanceHat). Thanks for the insights
that made this possible.

## License

[Apache License 2.0](LICENSE)
