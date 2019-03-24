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
    "bridge_port": 7000,
    "subscription_inactive_limit_minutes": 15
  },
  "elasticsearch": {
    "host": "localhost",
    "port": 9200
  },
  "integrations": {
    "skywalking": {
      "host": "localhost",
      "port": 12800,
      "timezone": null,
      "endpoint_detection_interval_seconds": 15
    }
  }
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
| bridge_port                         | 7000      | Port used by plugin/tooltip to connect to core's eventbus    |
| subscription_inactive_limit_minutes | 15        | Minutes necessary of inactive to prune artifact subscriptions (-1 to disable) |

## Elasticsearch Settings

| Property | Value     | Description                                             |
| -------- | --------- | ------------------------------------------------------- |
| host     | localhost | The address of the Elasticsearch instance to connect to |
| port     | 9200      | The port of the Elasticsearch instance to connect to    |

## Integrations

### Apache SkyWalking

| Property                            | Value     | Description                                                  |
| ----------------------------------- | --------- | ------------------------------------------------------------ |
| host                                | localhost | The address of the SkyWalking OAP instance to connect to     |
| port                                | 12800     | The port of the SkyWalking OAP instance to connect to        |
| timezone                            | null      | Timezone to use when querying SkyWalking OAP (defaults to system) |
| endpoint_detection_interval_seconds | 15        | Seconds to wait before scanning for new endpoints            |

# Next Step

- [Install Source++ Plugin](./05-install-source-plugin.md)
