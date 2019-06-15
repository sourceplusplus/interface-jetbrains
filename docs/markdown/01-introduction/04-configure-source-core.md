# Source++ Core Configuration

```json
{
  "core": {
    "host": "localhost",
    "port": 8080,
    "ssl": false,
    "jks_path": null,
    "jks_password": null,
    "secure_mode": false,
    "api_key": null,
    "subscription_inactive_limit_minutes": 15
  },
  "storage": {
    "type": "h2",
    "elasticsearch": {
      "host": "localhost",
      "port": 9200
    }
  },
  "integrations": [
      {
        "id": "apache_skywalking",
        "category": "APM",
        "enabled": true,
        "version": "6.1.0",
        "connection": {
          "host": "localhost",
          "port": 12800
        },
        "config": {
          "timezone": null,
          "endpoint_detection_interval_seconds": 15
        }
      }
    ]
}
```

## Core Settings

| Property                            | Value     | Description                                                  |
| ----------------------------------- | --------- | ------------------------------------------------------------ |
| host                                | localhost | The address to listen on (use 0.0.0.0 for external access)   |
| port                                | 8080      | The port to listen on                                        |
| ssl                                 | false     | Enable https (must input `jks_path` if enabled)              |
| jks_path                            | null      | Path to the JKS file (`ssl` must be true to enable)          |
| jks_password                        | null      | Password for the JKS file (optional)                         |
| secure_mode                         | false     | Secure API with Bearer token authentication (requires `api_key`) |
| api_key                             | null      | The API key to use (`secure_mode` must be true to enable)    |
| subscription_inactive_limit_minutes | 15        | Minutes necessary of inactive to prune artifact subscriptions (-1 to disable) |

## Storage Settings

| Property | Value | Description                                              |
| -------- | ----- | -------------------------------------------------------- |
| type     | h2    | The storage system type to use (`h2` or `elasticsearch`) |

### Elasticsearch Settings

| Property | Value     | Description                                             |
| -------- | --------- | ------------------------------------------------------- |
| host     | localhost | The address of the Elasticsearch instance to connect to |
| port     | 9200      | The port of the Elasticsearch instance to connect to    |

## Integrations

### Apache SkyWalking

| Property                            | Value             | Description                                                  |
| ----------------------------------- | ----------------- | ------------------------------------------------------------ |
| id                                  | apache_skywalking | ID for the given Source++ integration                        |
| category                            | APM               | Category for the given Source++ integration                  |
| enabled                             | false             | Used to enable/disable the given Source++ integration        |
| version                             | 6.1.0             | Current version of the Source++ integration                  |

#### Connection

| Property                            | Value     | Description                                                  |
| ----------------------------------- | --------- | ------------------------------------------------------------ |
| host                                | localhost | The address of the SkyWalking OAP instance to connect to     |
| port                                | 12800     | The port of the SkyWalking OAP instance to connect to        |

#### Config

| Property                            | Value     | Description                                                  |
| ----------------------------------- | --------- | ------------------------------------------------------------ |
| timezone                            | null      | Timezone to use when querying SkyWalking OAP (defaults to system) |
| endpoint_detection_interval_seconds | 15        | Seconds to wait before scanning for new endpoints            |

# Next Step

- [Install Source++ Plugin](./05-install-source-plugin.md)
