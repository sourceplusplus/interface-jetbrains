# 0.2.0-alpha (2019-05-31)

- Moved GitHub project from CodeBrig/Source to sourceplusplus/Assistant
- Added additional query time frames (5 minutes, 1 hour, 3 hours)
- Renamed `pingEndpointEnabled` core config to `pingEndpointAvailable`
- Docker setup guide Knowledge Base ([#8](https://github.com/sourceplusplus/Assistant/issues/8))
- Configurable environment setup (via Docker) in Source++ Plugin Source++ Plugin ([#20](https://github.com/sourceplusplus/Assistant/issues/20))
- Source++ Core Docker image on Docker Hub Source++ Core Source++ Plugin ([#15](https://github.com/sourceplusplus/Assistant/issues/15))
- Add automatic release artifact uploads ([#66](https://github.com/sourceplusplus/Assistant/issues/66))
- Use RequestMapping.method in determining correct artifact Source++ Agent Source++ Core ([#10](https://github.com/sourceplusplus/Assistant/issues/10))
- Upgrade to SkyWalking 6.1 Source++ Agent Source++ Core ([#30](https://github.com/sourceplusplus/Assistant/issues/30))
- Fix bridge reconnection issue ([#61](https://github.com/sourceplusplus/Assistant/issues/61))
- Fix portal pre-loading Source++ Portal bug ([#57](https://github.com/sourceplusplus/Assistant/issues/57))
- Remove inactive portals Source++ Portal ([#59](https://github.com/sourceplusplus/Assistant/issues/59))
- H2 database storage Source++ Core ([#14](https://github.com/sourceplusplus/Assistant/issues/14))
- Application/environment switching Source++ Plugin Source++ Portal ([#16](https://github.com/sourceplusplus/Assistant/issues/16))
- Get rid of eventbus bridge port ([#56](https://github.com/sourceplusplus/Assistant/issues/56))
- IDE restart required after S++ Core connection Source++ Plugin bug ([#26](https://github.com/sourceplusplus/Assistant/issues/26))
- Implement artifact config tab in tooltip UI Source++ Portal ([#13](https://github.com/sourceplusplus/Assistant/issues/13))
- Register external portals as new Source++ Portal ([#52](https://github.com/sourceplusplus/Assistant/issues/52))
- Change portalId to portalUuid Source++ Portal ([#50](https://github.com/sourceplusplus/Assistant/issues/50))
- Ability to change portal active tab Source++ Portal ([#51](https://github.com/sourceplusplus/Assistant/issues/51))
- Add status column to trace stack table Source++ Portal ([#46](https://github.com/sourceplusplus/Assistant/issues/46))
- Truncate extra 0 in time occurred on Traces tab Source++ Portal ([#45](https://github.com/sourceplusplus/Assistant/issues/45))
- View artifact slowest traces Source++ Core Source++ Plugin Source++ Portal ([#19](https://github.com/sourceplusplus/Assistant/issues/19))
- Rename Source++ Tooltip to Source++ Portal ([#49](https://github.com/sourceplusplus/Assistant/issues/49))
- Upgrade Semantic UI and create tooltip build task Source++ Portal ([#43](https://github.com/sourceplusplus/Assistant/issues/43))
- Responsive Source++ Tooltip UI Source++ Portal ([#44](https://github.com/sourceplusplus/Assistant/issues/44))

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
