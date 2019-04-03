The Metric API allows you to store and retrieve application metrics. You can use this API to automatically synchronize the data in Source++ with your own system, without any human interference.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method   | Address                                                                                                       | Description                                |
|----------|-------------------------------------------------------------------------------------------------------------- |--------------------------------------------|
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/subscribe](#subscribe-artifact-metrics)     | Subscribe to source artifact's metrics     |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/unsubscribe](#unsubscribe-artifact-metrics) | Unsubscribe from source artifact's metrics |

---------------------------------------------------------------------------------------------------------------------------------


# Subscribe Artifact Metrics
**Endpoint [PUT]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/subscribe](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/subscribe)

**Description**: Subscribe to source artifact's metrics.

**Request**:
```json
{
   "time_frame": "LAST_15_MINUTES",
   "metric_types":[
      "Throughput_Average", "ResponseTime_Average", "ServiceLevelAgreement_Average",
      "ResponseTime_99Percentile", "ResponseTime_95Percentile", "ResponseTime_90Percentile",
      "ResponseTime_75Percentile", "ResponseTime_50Percentile"
   ]
}
```

**Response**:
```
n/a (200 OK)
```

# Unsubscribe Artifact Metrics
**Endpoint [PUT]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/unsubscribe](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/metrics/unsubscribe)

**Description**: Unsubscribe from source artifact's metrics.

**Request**:

```json
{
   "metric_types":[
      "Throughput_Average", "ResponseTime_Average", "ServiceLevelAgreement_Average",
      "ResponseTime_99Percentile", "ResponseTime_95Percentile", "ResponseTime_90Percentile",
      "ResponseTime_75Percentile", "ResponseTime_50Percentile"
   ]
}
```

**Response**:
```
n/a (200 OK)
```
