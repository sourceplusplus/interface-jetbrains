# 0.2.5-alpha (todo)

## Dependency upgrade
- Upgraded jackson (2.10.2 -> 2.10.3)
- Upgraded JetBrains annotations (18.0.0 -> 19.0.0)
- Upgraded log4j (2.13.0 -> 2.13.1)
- Upgraded okhttp (3.14.6 -> 4.4.1)
- Upgraded JetBrains intellij plugin (0.4.17 -> 0.4.18)

# 0.2.4-alpha (2020-03-29)

## Bug
- Fixed issue with "Parent Stack" not navigating back to caller function
- Fixed issue with environment dialog not recognizing activate environment when updated
- Disable activate button on first saved environment ([#110](https://github.com/sourceplusplus/Assistant/issues/110))
- Disabled buildSearchableOptions (causes HeadlessException on Travis CI)
- Fixed missing logging issue when using "Setup via Docker"

## Improvement
- Registered `SourcePluginConsoleService` to dispose on project disposal
- Replaced deprecated `PluginSettingsComponent` for `PropertiesComponent` implementation
- Auto-attach agent to Gradle based application runs ([#116](https://github.com/sourceplusplus/Assistant/issues/116))
- Build with JDK 11 instead of JDK 8
- Use @Slf4j instead of static final Loggers
- Specify charset when using URLDecoder.encode/URLDecoder.decode
- Allow Travis CI to use default Docker Compose
- Force Travis CI to use https.protocols TLSv1.2
- Use ignore variable names on catch clauses where appropriate
- Various code refactorings
- Added gradle, Java, and properties to IntelliJ plugin depends
- Added IntelliJ plugin icon for marketplace display
- Added logging to Portal overview page
- Removed unused handler from Portal overview page
- Updated agent log format to include more information

## Dependency upgrade
- Upgraded Groovy (2.4.15 -> 2.4.17)
- Upgraded IntelliJ plugin (2018.3.6 -> 2019.3.4)
- Upgraded Apache ECharts (4.2.1 -> 4.7.0)
- Upgraded Moment.js (2.20.1 -> 2.24.0)
- Upgraded SockJS (1.3.0 -> 1.4.0)
- Upgraded vertx-eventbus.min.js (n/a -> 3.8.3)
- Upgraded from Semantic 2.4.0 to Fomantic UI 2.8.4
- Upgraded JetBrains intellij plugin (0.4.16 -> 0.4.17)
- Upgraded GitSocratic (5d33d30262 -> b50580b3c7)

# 0.2.3-alpha (2020-02-14)

## Bug
- Fixed issue with agent failing to connect to Apache SkyWalking when using host `skywalking-oap`

## Improvement
- Switched docs from using GitHub `access_token` query param to basic authentication
- Removed usage of deprecated `ApplicationComponent`
- Added additional logging output to Travis CI builds
- Added favicon.ico redirect to docs
- Using `skywalking-oap` as Apache SkyWalking Docker host universally
- Groovy/Kotlin no longer optional IntelliJ plugins
- Removed usage of `Json.mapper.findAndRegisterModules`
- Force Travis CI to use JDK8
- Removed unused files from agent jar

## Dependency upgrade
- Upgraded Apache SkyWalking (6.2.0 -> 6.5.0)
- Upgraded Vert.x (3.7.1 -> 3.8.5)
- Upgraded Journey (0.3.3 -> 0.4.0)
- Upgraded proguard-gradle (5.3.3 -> 6.2.2)
- Upgraded log4j (2.10.0 -> 2.13.0)
- Upgraded slf4j (1.7.25 -> 1.7.30)
- Upgraded asm (5.2 -> 7.3.1)
- Upgraded asm-nonclassloadingextensions (1.0-rc1 -> 1.0-rc2)
- Upgraded gradle-apt-plugin (0.7 -> 0.21)
- Upgraded immutables (2.5.6 -> 2.8.3)
- Upgraded okhttp (3.9.1 -> 3.14.6)
- Upgraded junit (4.12 -> 4.13)
- Upgraded gson (2.8.5 -> 2.8.6)
- Upgraded guava (26.0-jre -> 28.2-jre)
- Upgraded JetBrains annotations (13.0 -> 18.0.0)
- Upgraded jackson (2.9.9 -> 2.10.2)
- Upgraded metrics-core (4.1.0 -> 4.1.2)
- Upgraded log4j (2.11.1 -> 2.13.0)
- Upgraded h2 (1.4.199 -> 1.4.200)
- Upgraded JetBrains intellij plugin (0.4.9 -> 0.4.16)
- Upgraded IntelliJ plugin IntelliJ version (2018.3.5 -> 2018.3.6)
- Upgraded GitSocratic (92bd34ab56 -> 5d33d30262)

# 0.2.2-alpha (2019-07-14)

- Upgraded Journey browser version to 0.3.3
- Increased IntelliJ Linux plugin compatibility ([#68](https://github.com/sourceplusplus/Assistant/issues/68))
- Cloned portal views share data instead of copying ([#91](https://github.com/sourceplusplus/Assistant/issues/91))

# 0.2.1-alpha (2019-07-04)

- Added ability to switch overview graph ([#18](https://github.com/sourceplusplus/Assistant/issues/18))
- Configurable latest/slowest trace page size ([#65](https://github.com/sourceplusplus/Assistant/issues/65))
- Upgrade to SkyWalking 6.2 ([#97](https://github.com/sourceplusplus/Assistant/issues/97))
- Added agent artifact trigger metrics ([#83](https://github.com/sourceplusplus/Assistant/issues/83))
- Fixed secure mode sending incorrect integration ports ([#80](https://github.com/sourceplusplus/Assistant/issues/80))
- Added AdminAPI.shutdown API endpoint ([#85](https://github.com/sourceplusplus/Assistant/issues/85))
- Implemented ability for Source++ to monitor itself ([#81](https://github.com/sourceplusplus/Assistant/issues/81))
- Closing files now correctly unsubscribe artifacts ([#86](https://github.com/sourceplusplus/Assistant/issues/86))
- Unsubscribed artifacts now correctly close internal portal ([#95](https://github.com/sourceplusplus/Assistant/issues/95))
- Force unique application names ([#88](https://github.com/sourceplusplus/Assistant/issues/88))
- Ability to configure agent with application name instead of id ([#89](https://github.com/sourceplusplus/Assistant/issues/89))
- Better portal caching ([#70](https://github.com/sourceplusplus/Assistant/issues/70))
- More efficient SkyWalking endpoint sync mechanism ([#90](https://github.com/sourceplusplus/Assistant/issues/90))
- Increased ability to change portal theme ([#87](https://github.com/sourceplusplus/Assistant/issues/87))
- Skip tracing Groovy generated functions ([#84](https://github.com/sourceplusplus/Assistant/issues/84))

# 0.2.0-alpha (2019-06-18)

- Moved GitHub project from CodeBrig/Source to sourceplusplus/Assistant
- Added additional query time frames (5 minutes, 1 hour, 3 hours)
- Renamed `pingEndpointEnabled` core config to `pingEndpointAvailable`
- Added documentation for Admin API
- Docker setup guide Knowledge Base ([#8](https://github.com/sourceplusplus/Assistant/issues/8))
- Configurable environment setup (via Docker) in Source++ Plugin ([#20](https://github.com/sourceplusplus/Assistant/issues/20))
- Source++ Core Docker image on Docker Hub ([#15](https://github.com/sourceplusplus/Assistant/issues/15))
- Add automatic release artifact uploads ([#66](https://github.com/sourceplusplus/Assistant/issues/66))
- Use RequestMapping.method in determining correct artifact ([#10](https://github.com/sourceplusplus/Assistant/issues/10))
- Upgrade to SkyWalking 6.1 ([#30](https://github.com/sourceplusplus/Assistant/issues/30))
- Fix bridge reconnection issue ([#61](https://github.com/sourceplusplus/Assistant/issues/61))
- Fix portal pre-loading bug ([#57](https://github.com/sourceplusplus/Assistant/issues/57))
- Remove inactive portals ([#59](https://github.com/sourceplusplus/Assistant/issues/59))
- H2 database storage ([#14](https://github.com/sourceplusplus/Assistant/issues/14))
- Application/environment switching ([#16](https://github.com/sourceplusplus/Assistant/issues/16))
- Get rid of eventbus bridge port ([#56](https://github.com/sourceplusplus/Assistant/issues/56))
- IDE restart required after S++ Core connection bug ([#26](https://github.com/sourceplusplus/Assistant/issues/26))
- Implement artifact config tab in tooltip UI ([#13](https://github.com/sourceplusplus/Assistant/issues/13))
- Register external portals as new ([#52](https://github.com/sourceplusplus/Assistant/issues/52))
- Change portalId to portalUuid ([#50](https://github.com/sourceplusplus/Assistant/issues/50))
- Ability to change portal active tab ([#51](https://github.com/sourceplusplus/Assistant/issues/51))
- Add status column to trace stack table ([#46](https://github.com/sourceplusplus/Assistant/issues/46))
- Truncate extra 0 in time occurred on Traces tab ([#45](https://github.com/sourceplusplus/Assistant/issues/45))
- View artifact slowest traces ([#19](https://github.com/sourceplusplus/Assistant/issues/19))
- Rename Source++ Tooltip to Source++ Portal ([#49](https://github.com/sourceplusplus/Assistant/issues/49))
- Upgrade Semantic UI and create tooltip build task ([#43](https://github.com/sourceplusplus/Assistant/issues/43))
- Responsive Source++ Tooltip UI ([#44](https://github.com/sourceplusplus/Assistant/issues/44))
- Core standalone functionality ([#60](https://github.com/sourceplusplus/Assistant/issues/60))
- Automatic GitHub release artifact uploads via Travis CI ([#66](https://github.com/sourceplusplus/Assistant/issues/66))
- Added ability for agent to detect integration connection information via API ([#73](https://github.com/sourceplusplus/Assistant/issues/73))

# 0.1.4-alpha (2019-03-30)

- Fixed issue with overview tab not being updated when viewing an artifact with no data
- Added `forceSubscribe` to SourceArtifactConfig
- Improved ability to find Source++ Plugin in JetBrains Plugins Repository
- Improved ability to create automatic builds
- Improved error message for incorrect API token when connecting to Source++ Core

# 0.1.3-alpha (2019-03-25)

- Fixed `core.integrations.skywalking.port` to point to `12800` instead of `12799`

# 0.1.2-alpha (2019-03-24)

- Improved Source++ Core ability to reconnect when in secure mode
- Created `artifact_subscriptions` storage for persistent subscriptions between Source++ Core reboots
- Created `core.integrations` structure in Source++ Core config
- Added `core.integrations.skywalking.timezone` for configuring timezone used when querying SkyWalking (defaults to system)
- Changed default timezone from UTC to system default when querying SkyWalking
- Added `core.integrations.skywalking.endpoint_detection_interval_seconds` for configuring endpoint detection interval
- Lowered default endpoint detection interval from every 30 seconds to every 15 seconds
- Renamed configuration `core.subscription_inactive_limit` to `core.subscription_inactive_limit_minutes`
- Fixed invalid file bug in Source++ Plugin which threw when dealing with recently deleted files
- Fixed some 404 links in knowledge base
- Made Groovy plugin optional in Source++ Plugin
- Added SpringMVC's `PostMapping`, `PutMapping`, `DeleteMapping`, and `PatchMapping` to auto-subscribed annotations
- Fixed issue with duplicate application subscription entries in the `Get Application Subscriptions` call
- Fixed issue with Source++ Agent adding unnecessary tracing to auto-subscribed artifacts
- Added `/admin/storage/refresh` endpoint (used for integration tests)
- Modified doc-server to use absolute links for knowledge base hrefs

# 0.1.1-alpha (2019-03-07)

- Externalized Source++ Agent dependencies (via jarjar)
- Added intentions for subscribing/unsubscribing to/from all artifacts in a file
- Added build resource files to the various Source++ components
- Configurable Source++ Agent log location
- Skip monitoring getSkyWalkingDynamicField() methods
- Added integration info section to Source++ Core info endpoint
- Fixed issues with incorrect qualified name detection and shortening
- Small bug fixes

# 0.1.0-alpha (2019-03-01)

- Initial alpha release.
