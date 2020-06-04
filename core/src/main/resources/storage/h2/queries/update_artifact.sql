WITH existing_artifact(
  app_uuid, artifact_qualified_name, endpoint, subscribe_automatically, force_subscribe,
  module_name, component, endpoint_name, endpoint_ids, status
) AS (
    SELECT
      app_uuid, artifact_qualified_name, endpoint, subscribe_automatically, force_subscribe,
      module_name, component, endpoint_name, endpoint_ids, status
    FROM source_artifact
    WHERE 1=1
    AND app_uuid = ?
    AND artifact_qualified_name = ?
)
UPDATE source_artifact
SET
  endpoint = COALESCE (?, (SELECT endpoint FROM existing_artifact)),
  subscribe_automatically = COALESCE (?, (SELECT subscribe_automatically FROM existing_artifact)),
  force_subscribe = COALESCE (?, (SELECT force_subscribe FROM existing_artifact)),
  module_name = COALESCE (?, (SELECT module_name FROM existing_artifact)),
  component = COALESCE (?, (SELECT component FROM existing_artifact)),
  endpoint_name = COALESCE (?, (SELECT endpoint_name FROM existing_artifact)),
  endpoint_ids = COALESCE (?, (SELECT endpoint_ids FROM existing_artifact)),
  status = COALESCE (?, (SELECT status FROM existing_artifact))
WHERE 1=1
AND app_uuid = (SELECT app_uuid FROM existing_artifact)
AND artifact_qualified_name = (SELECT artifact_qualified_name FROM existing_artifact);