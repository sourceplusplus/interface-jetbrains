The Artifact API allows you to retrieve, update, and unsubscribe from source code artifacts (functions, classes, etc) and source code artifact configurations. You can use this API to automatically synchronize the data in Source++ with your own system, without any human interference.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method   | Address                                                                                                           | Description                                      |
|----------|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| POST     | [/applications/:appUuid/artifacts](#create-source-artifact)                                                       | Create source artifact                           |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/configuration](#update-source-artifact-configuration)    | Update source artifact configuration             |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/unsubscribe](#unsubscribe-source-artifact-subscriptions) | Unsubscribe from source artifact subscription(s) |

**GET** methods are used to fetch data. The following GET methods are available:

| Method   | Address                                                                                                     | Description                             |
|----------|-------------------------------------------------------------------------------------------------------------|-----------------------------------------|
| GET      | [/applications/:appUuid/artifacts](#get-application-artifacts)                                              | Get application-wide source artifacts   |
| GET      | [/applications/:appUuid/artifacts/:artifactQualifiedName](#get-source-artifact)                             | Get source artifact                     |
| GET      | [/applications/:appUuid/artifacts/:artifactQualifiedName/configuration](#get-source-artifact-configuration) | Get source artifact's configuration     |
| GET      | [/applications/:appUuid/artifacts/:artifactQualifiedName/subscriptions](#get-source-artifact-subscriptions) | Get source artifact's subscriptions     |

---------------------------------------------------------------------------------------------------------------------------------

# Create Source Artifact
**Endpoint [POST]**: [/applications/:appUuid/artifacts](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts)

**Description**: Register a source code artifact.

**Request [SourceArtifact]**:
```json
{
   "artifact_qualified_name":"com.company.TestClass.testMethod()"
}
```

**Response [SourceArtifact]**:
```json
{
   "artifact_qualified_name":"com.company.TestClass.testMethod()"
}
```

# Get Application Artifacts
**Endpoint [POST]**: [/applications/:appUuid/artifacts](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts)

**Description**: Register a source code artifact.

**Request [SourceArtifact]**:
```
n/a
```

**Response [SourceArtifact]**:
```json
[
  {
    "app_uuid": "fd4ba998-7ecc-40f5-b9cb-1880a3f1c93b",
    "artifact_qualified_name": "com.company.TestClass.testMethod()",
    "create_date": 1550800122.216000000,
    "last_updated": 1550800122.216000000,
    "config": {
      "endpoint": true,
      "subscribe_automatically": true,
      "module_name": "my-project",
      "component": "SpringMVC",
      "endpoint_name": "/hello/{name}"
    }
  }
]
```

# Get Source Artifact
**Endpoint [GET]**: [/applications/:appUuid/artifacts/:artifactQualifiedName](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName)

**Description**: Get source code artifact.

**Request**:
```
n/a
```

**Response [SourceArtifact]**:
```json
{
  "app_uuid": "fd4ba998-7ecc-40f5-b9cb-1880a3f1c93b",
  "artifact_qualified_name": "com.company.TestClass.testMethod()",
  "create_date": 1548025466.760000000,
  "last_updated": 1548025500.894000000,
  "config": {
    "endpoint": true,
    "endpoint_name": "/hello/{name}",
    "endpoint_ids": [
      "8"
    ]
  }
}
```

# Update Source Artifact Configuration
**Endpoint [PUT]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/configuration](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/config)

**Description**: Update source artifact configuration.

**Request [SourceArtifactConfig]**:
```json
{
   "endpoint_name":"/remote/{name}"
}
```

**Response [SourceArtifactConfig]**:
```json
{
   "endpoint_name":"/remote/{name}"
}
```

# Get Source Artifact Configuration
**Endpoint [GET]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/configuration](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/configuration)

**Description**: Get source code artifact configuration.

**Request**:
```
n/a
```

**Response [SourceArtifactConfig]**:
```json
{
   "endpoint_name":"/remote/{name}"
}
```

# Get Source Artifact Subscriptions
**Endpoint [GET]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/subscriptions](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/subscriptions)

**Description**: Get source code artifact subscriptions.

**Request**:
```
n/a
```

**Response ( SourceArtifactSubscriber[] )**:
```json
[
  {
    "subscriber_uuid": "fd4ba998-7ecc-40f5-b9cb-1880a3f1c93b",
    "subscription_last_accessed": {
      "METRICS": 1550887529.710000000,
      "TRACES": 1550887529.726000000
    }
  }
]
```
