SELECT
  artifact_qualified_name, trace_id, start_time, duration
FROM source_artifact_failure
WHERE 1=1
AND app_uuid = ?
AND artifact_qualified_name = ?
AND start_time >= ?
AND start_time <= ?
ORDER BY start_time DESC
LIMIT ?;