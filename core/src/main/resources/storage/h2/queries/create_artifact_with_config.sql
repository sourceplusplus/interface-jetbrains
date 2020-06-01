INSERT INTO source_artifact (
  app_uuid, artifact_qualified_name, create_date, endpoint, subscribe_automatically,
  force_subscribe, module_name, component, endpoint_name, endpoint_ids
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);