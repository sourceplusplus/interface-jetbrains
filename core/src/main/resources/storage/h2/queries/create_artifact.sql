INSERT INTO source_artifact (
  app_uuid, artifact_qualified_name, create_date, endpoint, auto_subscribe,
  module_name, component, endpoint_name, endpoint_ids, active_failing, latest_failed_service_instance
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);