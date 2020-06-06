MERGE INTO SOURCE_ARTIFACT_FAILURE(
  app_uuid, artifact_qualified_name, trace_id, start_time, duration
)
KEY(app_uuid, artifact_qualified_name, trace_id)
SELECT ?, ?, ?, ?, ?;