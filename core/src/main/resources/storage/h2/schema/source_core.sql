CREATE TABLE IF NOT EXISTS core_config
(
    json_data VARCHAR
);

CREATE TABLE IF NOT EXISTS source_application
(
    app_uuid     VARCHAR PRIMARY KEY,
    app_name     VARCHAR   NOT NULL UNIQUE,
    create_date  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    agent_config TEXT
);

CREATE TABLE IF NOT EXISTS source_artifact
(
    app_uuid                       VARCHAR   NOT NULL,
    artifact_qualified_name        VARCHAR   NOT NULL,
    create_date                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    endpoint                       BOOLEAN,
    auto_subscribe        BOOLEAN,
    module_name                    VARCHAR,
    component                      VARCHAR,
    endpoint_name                  VARCHAR,
    endpoint_ids                   VARCHAR,
    active_failing                 BOOLEAN,
    latest_failed_service_instance VARCHAR,
    PRIMARY KEY (app_uuid, artifact_qualified_name)
);

CREATE TABLE IF NOT EXISTS source_artifact_subscription
(
    subscriber_uuid         VARCHAR   NOT NULL,
    app_uuid                VARCHAR   NOT NULL,
    artifact_qualified_name VARCHAR   NOT NULL,
    subscription_type       VARCHAR   NOT NULL,
    subscription_data       VARCHAR   NOT NULL UNIQUE,
    last_accessed           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (subscriber_uuid, app_uuid, artifact_qualified_name, subscription_type, subscription_data)
);

CREATE TABLE IF NOT EXISTS source_artifact_failure
(
    app_uuid                VARCHAR   NOT NULL,
    artifact_qualified_name VARCHAR   NOT NULL,
    trace_id                VARCHAR   NOT NULL,
    start_time              TIMESTAMP NOT NULL,
    duration                INT,
    PRIMARY KEY (app_uuid, artifact_qualified_name, trace_id)
);