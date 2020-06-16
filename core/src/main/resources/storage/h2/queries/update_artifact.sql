WITH existing_artifact(
  app_uuid, artifact_qualified_name, endpoint, auto_subscribe,
  module_name, component, endpoint_name, endpoint_ids, active_failing, latest_failed_service_instance
) AS (
    SELECT
      app_uuid, artifact_qualified_name, endpoint, auto_subscribe,
      module_name, component, endpoint_name, endpoint_ids, active_failing, latest_failed_service_instance
    FROM source_artifact
    WHERE 1=1
    AND app_uuid = ?
    AND artifact_qualified_name = ?
)
UPDATE source_artifact
SET
  endpoint = COALESCE (?, (SELECT endpoint FROM existing_artifact)),
  auto_subscribe = COALESCE (?, (SELECT auto_subscribe FROM existing_artifact)),
  module_name = COALESCE (?, (SELECT module_name FROM existing_artifact)),
  component = COALESCE (?, (SELECT component FROM existing_artifact)),
  endpoint_name = COALESCE (?, (SELECT endpoint_name FROM existing_artifact)),
  endpoint_ids = COALESCE (?, (SELECT endpoint_ids FROM existing_artifact)),
  active_failing = COALESCE (?, (SELECT active_failing FROM existing_artifact)),
  latest_failed_service_instance = COALESCE (?, (SELECT latest_failed_service_instance FROM existing_artifact))
WHERE 1=1
AND app_uuid = (SELECT app_uuid FROM existing_artifact)
AND artifact_qualified_name = (SELECT artifact_qualified_name FROM existing_artifact);