# Source++ Agent Configuration

```json
{
  "enabled": true,
  "test_mode": false,
  "log_level": "info",
  "log_location": null,
  "application": {
    "app_uuid": null,
    "application_source_code": {
      "packages": []
    }
  },
  "api": {
    "version": "v1",
    "host": "localhost",
    "port": 8080,
    "ssl": false,
    "key": null
  },
  "skywalking": {
    "enabled": true,
    "sample_n_per_3_secs": -1,
    "span_limit_per_segment": 300,
    "output_enhanced_classes": false
  },
  "plugin-bridge": {
    "enabled": false,
    "host": "localhost",
    "port": 7000
  }
}
```

## General Settings

| Property     | Value | Description                       |
| ------------ | ----- | --------------------------------- |
| enabled      | true  | Enable/disable Source++ Agent     |
| test_mode    | false | Includes additional output        |
| log_level    | info  | Log level to use throughout agent |
| log_location | null  | The directory to write logs to    |

## Application Settings

| Property                | Value | Description                        |
| ----------------------- | ----- | ---------------------------------- |
| app_uuid                | null  | The application UUID               |
| application_source_code | null  | Represents the packages to monitor |

## API Settings

| Property | Value     | Description                 |
| -------- | --------- | --------------------------- |
| version  | v1        | API version                 |
| host     | localhost | API host                    |
| port     | 8080      | API port                    |
| ssl      | false     | Whether to use SSL or not   |
| key      | null      | Bearer token authentication |

## Apache SkyWalking Settings

| Property                | Value           | Description                              |
| ----------------------- | --------------- | ---------------------------------------- |
| enabled                 | localhost       | Enabled/disable Apache SkyWalking agent  |
| sample_n_per_3_secs     | -1              | Amount of traces to sample per 3 seconds |
| span_limit_per_segment  | 300             | Maximum span limit per segment           |
| output_enhanced_classes | false           | Whether to output enhanced classes       |

## Plugin Bridge Settings

| Property | Value     | Description                              |
| -------- | --------- | ---------------------------------------- |
| enabled  | false     | Enable/disable bridge to Source++ Plugin |
| host     | localhost | The host to bridge to                    |
| port     | 7000      | The port to bridge to                    |

# Next Step

- [Subscribe to Source Artifact](./09-subscribe-to-artifact.md)
