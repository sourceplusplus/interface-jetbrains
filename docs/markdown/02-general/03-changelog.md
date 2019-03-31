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
