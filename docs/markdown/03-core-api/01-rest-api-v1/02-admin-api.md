The Admin API allows you to modify Source++ system settings and integrations. You can use this API to automatically synchronize the data in Source++ with your own system, without any human interference.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method | Address                                                      | Description                                               |
| ------ | ------------------------------------------------------------ | --------------------------------------------------------- |
| PUT    | [/admin/integrations/:integrationId](#update-integration)    | Update Source++ integration                               |

**GET** methods are used to fetch data. The following GET methods are available:

| Method | Address                                                      | Description                                               |
| ------ | ------------------------------------------------------------ | --------------------------------------------------------- |
| GET    | [/admin/integrations](#get-integrations)                     | Get all available Source++ integrations                   |
| GET    | [/admin/integrations/apache_skywalking/searchForNewEndpoints](#search-for-new-endpoints) | Search Apache SkyWalking for new endpoints to track |
| GET    | [/admin/storage/refresh](#refresh-storage)                   | Refresh system storage                                    |

---------------------------------------------------------------------------------------------------------------------------------

# Update Integration
**Endpoint [PUT]**: [/admin/integrations/:integrationId](https://api.sourceplusplus.com/v1/admin/integrations/:integrationId)

**Description**: Update Source++ integration.

**Request [IntegrationInfo]**:
```json
{
	"enabled": true,
	"connections": {
		"REST": {
			"host": "localhost",
			"port": 12800
		},
		"gRPC": {
			"host": "localhost",
			"port": 11800
		}
	}
}
```

**Response [IntegrationInfo]**:
```json
{
  "category": "APM",
  "enabled": true,
  "version": "8.0.1",
  "connections": {
    "gRPC": {
      "host": "localhost",
      "port": 11800
    },
    "REST": {
      "host": "localhost",
      "port": 12800
    }
  },
  "config": {
    "service_detection_delay_seconds": 10,
    "endpoint_detection_interval_seconds": 10,
    "failing_artifact_detection_interval_seconds": 10
  },
  "id": "apache_skywalking"
}
```

# Get Integrations
**Endpoint [GET]**: [/admin/integrations](https://api.sourceplusplus.com/v1/admin/integrations)

**Description**: Get available Source++ integrations.

**Request**:
```
n/a
```

**Response ( IntegrationInfo[] )**:
```json
[
  {
    "name": "Apache SkyWalking",
    "category": "APM",
    "enabled": true,
    "version": "8.0.1",
    "connections": {
      "gRPC": {
        "host": "localhost",
        "port": 11800
      },
      "REST": {
        "host": "localhost",
        "port": 12800
      }
    },
    "config": {
      "service_detection_delay_seconds": 10,
      "endpoint_detection_interval_seconds": 10,
      "failing_artifact_detection_interval_seconds": 10
    },
    "id": "apache_skywalking"
  }
]
```

# Search For New Endpoints
**Endpoint [GET]**: [/admin/integrations/apache_skywalking/searchForNewEndpoints](https://api.sourceplusplus.com/v1/admin/integrations/apache_skywalking/searchForNewEndpoints)

**Description**: Search Apache SkyWalking for new endpoints to track.

**Request**:
```
n/a
```

**Response**:
```
n/a (200 OK)
```

# Refresh Storage
**Endpoint [GET]**: [/admin/storage/refresh](https://api.sourceplusplus.com/v1/admin/storage/refresh)

**Description**: Refresh system storage.

**Request**:
```
n/a
```

**Response**:
```
n/a (200 OK)
```
