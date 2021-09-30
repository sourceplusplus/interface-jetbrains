<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# SourceMarker Changelog

## [Unreleased]

## [0.3.0] - 2021-09-30
### Added
- Live instruments & live control bar
- Hide/show global source marks shortcut (Ctrl+Shift+D)
- Various dependency upgrades

### Fixed
- Issue determining endpoint name ([#358](https://github.com/sourceplusplus/SourceMarker/issues/358))

## [0.2.1] - 2020-03-23
### Added
- Synchronous/asynchronous source mark event listeners
- Portal opening event
- Ability to configure portal refresh interval
- Added debug console (hidden by default)

### Fixed
- Portal popup race condition ([#350](https://github.com/sourceplusplus/SourceMarker/issues/350))
- Issue with determining active service ([#285](https://github.com/sourceplusplus/SourceMarker/issues/285))

### Changed
- More intuitive portal default views

### Removed
- Local mentor module

### Upgraded
- Kotlin (1.4.30 -> 1.4.32)
- Vert.x (4.0.2 -> 4.0.3)
- gradle-docker-compose-plugin (0.14.0 -> 0.14.1)
- detekt (1.15.0 -> 1.16.0)
- JGit (5.10.0.202012080955-r -> 5.11.0.202103091610-r)
- Apache Commons Lang (3.11 -> 3.12.0)
- JUnit (4.13.1 -> 4.13.2)
- Apollo Android (2.5.3 -> 2.5.5)
- Dropwizard Metrics (4.1.17 -> 4.1.18)
- JetBrains IntelliJ plugin (0.6.5 -> 0.7.2)
- JetBrains changelog plugin (1.1.1 -> 1.1.2)
- kotlinx.coroutines (1.4.2 -> 1.4.3-native-mt)
- jOOQ (3.14.7 -> 3.14.8)
- Jackson (2.12.1 -> 2.12.2)
- kotlinx.serialization (1.0.1 -> 1.1.0)
- Guava (30.1-jre -> 30.1.1-jre)

## [0.2.0] - 2020-12-02
### Added
- Infinite scroll on traces portal ([#210](https://github.com/sourceplusplus/SourceMarker/issues/210))
- Implement localization bundling ([#288](https://github.com/sourceplusplus/SourceMarker/issues/288))
- Implemented logging portal page ([#230](https://github.com/sourceplusplus/SourceMarker/issues/230))
- Anonymous error reporting ([#115](https://github.com/sourceplusplus/SourceMarker/issues/115))
- Mentor task retry logic

### Changed
- Single page portal ([#254](https://github.com/sourceplusplus/SourceMarker/issues/254))
- Persist current view between internal portals ([#302](https://github.com/sourceplusplus/SourceMarker/issues/302))
- Dynamic portal host ports
- Refactored activity portal

### Upgraded
- Apache SkyWalking (8.2.0 -> 8.4.0)
- Vert.x (3.9.4 -> 4.0.2)
- Kotlin (1.4.10 -> 1.4.30)
- JGit (5.9.0.202009080501-r -> 5.10.0.202012080955-r)
- Guava (29.0-jre -> 30.1-jre)
- JetBrains annotations (19.0.0 -> 20.1.0)
- jOOQ (3.14.3 -> 3.14.7)
- kotlinx.coroutines (1.4.1 -> 1.4.2)
- kotlinx.datetime (0.1.0 -> 0.1.1)
- Apollo Android (2.4.4 -> 2.5.3)
- JetBrains IntelliJ plugin (0.6.3 -> 0.6.5)
- JetBrains changelog plugin (0.6.2 -> 1.1.1)
- Jackson (2.11.3 -> 2.12.1)
- Dropwizard Metrics (4.1.15 -> 4.1.17)
- kotlinx.serialization (1.0.0 -> 1.0.1)
- gradle-docker-compose-plugin (0.13.4 -> 0.14.0)
- detekt (1.14.2 -> 1.15.0)
- Apache ECharts (4.9.0 -> 5.0.1)

## [0.1.1] - 2020-12-01
### Fixed
- Incorrect timezone parsing ([#280](https://github.com/sourceplusplus/SourceMarker/issues/280))

## [0.1.0] - 2020-11-17
### Added
- Initial release