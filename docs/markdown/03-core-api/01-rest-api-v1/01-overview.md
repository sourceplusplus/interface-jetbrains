The REST API allows you to retrieve and update the data that is stored inside
Source++ from out of your own website or application. You can write your own scripts 
that send requests and instructions to our servers to fetch this data or
to update it. You can use this API to automatically synchronize the data in 
Source++ with your own system, without any human interference.

# Available Endpoints

The following table lists all methods that are accessible through the REST API.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method   | Address                                                                                                            | Description                                      |
|----------|--------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| POST     | [/applications](#create-application)                                                                               | Create application                               |
| PUT      | [/applications/:appUuid](#update-application)                                                                      | Update application                               |
| PUT      | [/applications/:appUuid/subscribers/:subscriberUuid/refresh](#refresh-subscriber-subscriptions)                    | Refresh subscriber's active subscriptions        |
| POST     | [/applications/:appUuid/artifacts](#create-source-artifact)                                                        | Create source artifact                           |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/config](#update-source-artifact-configuration)            | Update source artifact configuration             |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/unsubscribe](#unsubscribe-source-artifact-subscriptions)  | Unsubscribe from source artifact subscription(s) |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/subscribe](#subscribe-artifact-metrics)       | Subscribe to source artifact's metrics           |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/unsubscribe](#unsubscribe-artifact-metrics) | Unsubscribe from source artifact's metrics       |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/subscribe](#subscribe-artifact-traces)         | Subscribe to source artifact's traces            |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/unsubscribe](#unsubscribe-artifact-traces)   | Unsubscribe from source artifact's traces        |

**GET** methods are used to fetch data. The following GET methods are available:

| Method | Address                                                      | Description                           |
| ------ | ------------------------------------------------------------ | ------------------------------------- |
| GET    | [/applications/:appUuid](#get-application)                   | Get application                       |
| GET    | [/applications/:appUuid/subscriptions](#get-application-subscriptions) | Get application-wide subscriptions    |
| GET    | [/applications/:appUuid/artifacts](#get-application-artifacts) | Get application-wide source artifacts |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName](#get-source-artifact) | Get source artifact                   |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces](#get-artifact-traces) | Get traces for source artifact        |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans](#get-artifact-trace-span) | Get trace spans for source artifact   |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/config](#get-source-artifact-configuration) | Get source artifact configuration     |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/subscriptions](#get-source-artifact-subscriptions) | Get source artifact's subscriptions   |
