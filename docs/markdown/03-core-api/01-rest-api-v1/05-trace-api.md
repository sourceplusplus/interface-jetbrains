The Trace API allows you to store and retrieve distributed traces. You can use this API to automatically synchronize the data in Source++ with your own system, without any human interference.

**PUT** and **POST** methods are used to create and update data. The following PUT and POST methods are available:

| Method   | Address                                                                                                     | Description                               |
|----------|------------------------------------------------------------------------------------------------------------ |-------------------------------------------|
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/subscribe](#subscribe-artifact-traces)     | Subscribe to source artifact's traces     |
| PUT      | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/unsubscribe](#unsubscribe-artifact-traces) | Unsubscribe from source artifact's traces |

**GET** methods are used to fetch data. The following GET methods are available:

| Method | Address                                                      | Description                         |
| ------ | ------------------------------------------------------------ | ----------------------------------- |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces](#get-artifact-traces) | Get traces for source artifact      |
| GET    | [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans](#get-artifact-trace-span) | Get trace spans for source artifact |

---------------------------------------------------------------------------------------------------------------------------------

# Subscribe Artifact Traces
**Endpoint [PUT]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/subscribe](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/traces/subscribe)

**Description**: Subscribe to source artifact's traces.

**Request [ArtifactTraceSubscribeRequest]**:
```json
{
   "time_frame": "LATEST_TRACES"
}
```

**Response**:
```
n/a (200 OK)
```

# Unsubscribe Artifact Traces
**Endpoint [PUT]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/unsubscribe](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/traces/unsubscribe)

**Description**: Unsubscribe from source artifact's traces.

**Request**:
```json
{
   "trace_types":[
      "Manual_Trace"
   ]
}
```

**Response**:
```
n/a (200 OK)
```

# Get Artifact Traces
**Endpoint [GET]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/traces](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/traces)

**Description**: Get traces for source artifact.

**Request**:
```
n/a
```

**Response**:
```json
{
  "traces": [
    {
      "key": "2.1.15503689361100222",
      "operation_names": [
        "LoopAdder.calculateSum()"
      ],
      "duration": 770,
      "start": 1550368936110,
      "trace_ids": [
        "2.1.15503689361100223"
      ],
      "error": false
    }
  ],
  "total": 100
}
```

# Get Artifact Trace Spans
**Endpoint [PUT]**: [/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans](https://api.sourceplusplus.com/v1/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans)

**Description**: Get trace spans for source artifact.

**Request**:
```
n/a
```

**Response**:
```json
{
  "trace_spans": [
    {
      "trace_id": "2.47.15508851012630001",
      "segment_id": "2.47.15508851012630000",
      "span_id": 0,
      "parent_span_id": -1,
      "refs": [],
      "service_code": "18fa0a62-3c64-4562-81d9-247632fe166c",
      "start_time": 1550885101263,
      "end_time": 1550885101283,
      "endpoint_name": "/test",
      "artifact_qualified_name": "LoopAdder.calculateSum()",
      "type": "Local",
      "peer": "",
      "component": "",
      "layer": "Unknown",
      "tags": {},
      "logs": [],
      "error": false
    }
  ],
  "total": 12
}
```
