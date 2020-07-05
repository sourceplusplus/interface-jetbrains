package com.sourceplusplus.api.model.error;

/**
 * Possible errors core API is able to throw.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
public interface SourceAPIErrors {

    String INVALID_INPUT = "Invalid input";
    String MISSING_ACCOUNT_ID = "No userId supplied";
    String INVALID_APPLICATION_ID = "Invalid application id";
    String MISSING_SOURCE_ARTIFACT_CONFIG = "Missing source artifact config";
}
