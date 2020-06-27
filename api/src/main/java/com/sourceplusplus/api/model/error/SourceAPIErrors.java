package com.sourceplusplus.api.model.error;

/**
 * Possible errors core API is able to throw.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
public interface SourceAPIErrors {

    String INVALID_INPUT = "Invalid input";
    String MISSING_ACCOUNT_ID = "No userId supplied";
    String INVALID_APPLICATION_ID = "Invalid application id";
    String MISSING_SOURCE_ARTIFACT_CONFIG = "Missing source artifact config";
}
