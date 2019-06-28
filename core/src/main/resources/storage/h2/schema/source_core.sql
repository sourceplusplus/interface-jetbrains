CREATE TABLE source_application (
  app_uuid      VARCHAR PRIMARY KEY,
  app_name      VARCHAR NOT NULL UNIQUE,
  create_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  agent_config  TEXT
);

CREATE TABLE source_artifact (
  app_uuid                VARCHAR NOT NULL,
  artifact_qualified_name VARCHAR NOT NULL,
  create_date             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_updated            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  endpoint                BOOLEAN,
  subscribe_automatically BOOLEAN,
  force_subscribe         BOOLEAN,
  module_name             VARCHAR,
  component               VARCHAR,
  endpoint_name           VARCHAR,
  endpoint_id             VARCHAR,
  PRIMARY KEY(app_uuid, artifact_qualified_name)
);

CREATE TABLE source_artifact_subscription (
  subscriber_uuid         VARCHAR NOT NULL,
  app_uuid                VARCHAR NOT NULL,
  artifact_qualified_name VARCHAR NOT NULL,
  subscription_type       VARCHAR NOT NULL,
  last_accessed           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(subscriber_uuid, app_uuid, artifact_qualified_name, subscription_type)
);