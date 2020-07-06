<p align="center">
  <img src="docs/images/source_plus_plus_logo.png" width="450px" title="Source++">
</p>

*<p align="center">Open-source Automated Programming Assistant</p>*

<p align="center">
  <a href="https://travis-ci.com/sourceplusplus/Assistant"><img src="https://travis-ci.com/sourceplusplus/Assistant.svg?token=ss9XQPnrp2cb7kvLpwGX&branch=master"/></a>
  <a href="https://gitter.im/sourceplusplus"><img src="https://badges.gitter.im/Join Chat.svg"/></a>
  <a href="https://github.com/sourceplusplus/Assistant/blob/master/LICENSE"><img src="https://img.shields.io/badge/license-Apache 2-blue.svg?style=flat"/></a>
</p>

<p align="center">
  <a href="https://sourceplusplus.com">Website</a> ::
  <a href="docs/markdown">Knowledge Base</a> ::
  <a href="docs/markdown/02-general/02-roadmap.md">Roadmap</a>
</p>

---

## Introduction

Source++ is an open-source automated programming assistant with runtime-aware and context-aware functionality. Source++ is an observant-only programming assistant and aims to bridge application performance management (APM) solutions with the software developer's integrated development environment (IDE), enabling tighter feedback loops. Source++ can be used to help debug and prevent runtime issues by providing contextualized behavior about your source code throughout development.

### Why?

> 1. To enable developers easy access to feedback-driven development methodologies
> 2. APMs often lack tight integration with actual development of the source code they monitor
> 3. Source code comprehension can be improved by providing an extra dimension of behavior linked with said source code (ie. being able to visualize how a particular method acts in certain environments vs. locally)
> 4. No websites or dashboards necessary to debug complex asynchronous runtime issues (just look at and follow through your actual source code)
> 5. AI-based pair programming is going to be pretty cool when we get it right

## Augments

### Integrated Runtime Performance

![](https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/Integrated-Runtime-Performance.jpg)

#### User Interfaces

<table>
  <tr>
      <td width="20%" align="center"><b>Overview</b></td>
      <td width="20%" align="center"><b>Activity</b></td>
      <td width="20%" align="center"><b>Latest Traces</b></td>
      <td width="20%" align="center"><b>Trace Stack</b></td>
      <td width="20%" align="center"><b>Span Info</b></td>
  </tr>
  <tr>
     <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/IRP-Overview.jpg"/></td>
     <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/IRP-Activity.jpg"/></td>
     <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/IRP-Latest-Traces.jpg"/></td>
     <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/IRP-Trace-Stack.jpg"/></td>
    <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/IRP-Span-Info.jpg"/></td>
  </tr>
</table>

#### Inlay Marks

<table>
  <tr>
      <td width="50%" align="center"><b>Live Telemetry</b></td>
      <td width="50%" align="center"><b>Error Tracking</b></td>
  </tr>
  <tr>
     <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/entry_method_inlay.jpg"/></td>
     <td><img src="https://raw.githubusercontent.com/sourceplusplus/Assistant/master/docs/images/augments/irp/failing_artifact_inlay.jpg"/></td>
  </tr>
</table>

#### Gutter Marks

| Mark                          | Meaning                                                  |
| ----------------------------- | -------------------------------------------------------- |
| ![](docs/images/plugin/icons/entry_method/active_entry_method.svg) | Method handles user requests (SOAP, REST, etc) |
| ![](docs/images/plugin/icons/failing_method/failing_method.svg) | Method has thrown exception in application's most recent run |

## Getting Started

### Prerequisites

- Supported APMs: [Apache SkyWalking](https://github.com/apache/skywalking)
- Supported IDEs: JetBrains
- Supported OSes: Linux, macOS, and Windows

### Installation

#### Downloading the IDE

Install a JetBrains IDE if you don’t already have one.

-  [Download IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- Or [find another JetBrains product](https://www.jetbrains.com/products.html)

#### Install the Source++ Plugin

1. Press `Ctrl+Alt+S` to open the `Settings` dialog and then go to `Plugins`.
2. Click the `Install JetBrains plugin`.
3. In the dialog that opens, search for `SourcePlusPlus` ([direct link](https://plugins.jetbrains.com/plugin/12033-source-)).
4. Press `Install`.
5. Click `OK` in the `Settings` dialog and restart your IDE.

After installation, you will need to configure the Source++ Plugin (see [Configure Plugin](docs/markdown/01-introduction/06-configure-source-plugin.md)).

## Project Structure

| Internal Module               | Description                                              | Language    |
| ----------------------------- | -------------------------------------------------------- | ----------- |
| :api                          | Holds common data models and communication clients       | Java 8+     |
| :core                         | Handles integrations, contextualization, & subscriptions | Groovy 2.5+ |
| :plugin:jetbrains-plugin      | JetBrains implementation of the Source++ Plugin          | Groovy 2.5+ |
| :portal                       | Used to visually display contextualized artifact data    | Groovy 2.5+ |

| External Module               | Description                                              | Language    |
| ----------------------------- | -------------------------------------------------------- | ----------- |
| [SourceMarker](https://github.com/sourceplusplus/SourceMarker) | Used to tie visual marks & popups to source code artifacts | Kotlin 1.3+ |

## Building/Testing

#### Run all Source++ tests:
```
./gradlew test
```

#### Build Source++ Plugin:
```
./gradlew buildPlugin
```

#### Run Source++ Core (in Docker):
```
./gradlew runCore
```

#### Run Source++ Plugin (in JetBrains IDE):
```
./gradlew runIde
```

## Documentation

For full documentation, visit the [knowledge base](docs/markdown).

## Roadmap

To know what is going on, see the [roadmap](docs/markdown/02-general/02-roadmap.md).

## Contributing

Feel free to open issues on just about anything related to Source++.

## Attribution

This project was highly influenced by [PerformanceHat](https://github.com/sealuzh/PerformanceHat). Thanks for the insights
that made this possible.

## License

[Apache License 2.0](https://github.com/sourceplusplus/Assistant/blob/master/LICENSE)

