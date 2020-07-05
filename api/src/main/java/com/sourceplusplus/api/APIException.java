package com.sourceplusplus.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an exception thrown by Source++ Core API.
 *
 * @version 0.3.1
 * @since 0.1.4
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
public class APIException extends RuntimeException {

    private final Set<String> errors;

    public APIException(String... errors) {
        super(Arrays.toString(errors));
        this.errors = new HashSet<>(Arrays.asList(errors));
    }

    public boolean isUnauthorizedAccess() {
        return errors.contains("Unauthorized access");
    }
}
