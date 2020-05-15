package com.sourceplusplus.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an exception thrown by Source++ Core API.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.4
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
