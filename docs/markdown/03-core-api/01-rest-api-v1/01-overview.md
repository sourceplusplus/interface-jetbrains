The REST API allows you to retrieve and update the data that is stored inside Source++ from out of your own website or application. You can write your own scripts that send requests and instructions to our servers to fetch this data or to update it. You can use this API to automatically synchronize the data in Source++ with your own system, without any human interference.

# Available Endpoints

The following table lists all methods that are accessible through the REST API.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method   | Address                                                                                                            | Description                                      |
|----------|--------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| PUT      | [/admin/integrations/:integrationId](02-admin-api.md/#update-integration)                                                                | Update Source++ integration                      |
| POST     | [/applications](03-application-api.md/#create-application)                                                                               | Create application                               |
| PUT      | [/applications/:appUuid](03-application-api.md/#update-application)                                                                      | Update application                               |
| PUT      | [/applications/:appUuid/subscribers/:subscriberUuid/refresh](03-application-api.md/#refresh-subscriber-subscriptions)                    | Refresh subscriber's active subscriptions        |
| POST     | [/applications/:appUuid/artifacts](04-artifact-api.md/#create-source-artifact)                                                        | Create source artifact                           |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/config](04-artifact-api.md/#update-source-artifact-configuration)            | Update source artifact configuration             |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/unsubscribe](04-artifact-api.md/#unsubscribe-source-artifact-subscriptions)  | Unsubscribe from source artifact subscription(s) |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/subscribe](05-metric-api.md/#subscribe-artifact-metrics)       | Subscribe to source artifact's metrics           |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/unsubscribe](05-metric-api.md/#unsubscribe-artifact-metrics) | Unsubscribe from source artifact's metrics       |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/subscribe](06-trace-api.md/#subscribe-artifact-traces)         | Subscribe to source artifact's traces            |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/unsubscribe](06-trace-api.md/#unsubscribe-artifact-traces)   | Unsubscribe from source artifact's traces        |

**GET** methods are used to fetch data. The following GET methods are available:

| Method | Address                                                      | Description                           |
| ------ | ------------------------------------------------------------ | ------------------------------------- |
| GET    | [/admin/integrations](02-admin-api.md/#get-integrations)                     | Get all available Source++ integrations                   |
| GET    | [/admin/integrations/apache_skywalking/searchForNewEndpoints](02-admin-api.md/#search-for-new-endpoints) | Search Apache SkyWalking for new endpoints to track |
| GET    | [/admin/storage/refresh](02-admin-api.md/#refresh-storage)                   | Refresh system storage                                    |
| GET    | [/applications/:appUuid](03-application-api.md/#get-application)                   | Get application                       |
| GET    | [/applications/:appUuid/subscriptions](03-application-api.md/#get-application-subscriptions) | Get application-wide subscriptions    |
| GET    | [/applications/:appUuid/artifacts](03-application-api.md/#get-application-artifacts) | Get application-wide source artifacts |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName](04-artifact-api.md/#get-source-artifact) | Get source artifact                   |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces](06-trace-api.md/#get-artifact-traces) | Get traces for source artifact        |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans](06-trace-api.md/#get-artifact-trace-spans) | Get trace spans for source artifact   |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/config](04-artifact-api.md/#get-source-artifact-configuration) | Get source artifact configuration     |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/subscriptions](04-artifact-api.md/#get-source-artifact-subscriptions) | Get source artifact's subscriptions   |
