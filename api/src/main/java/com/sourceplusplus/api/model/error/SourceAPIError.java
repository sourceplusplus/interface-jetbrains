package com.sourceplusplus.api.model.error;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an error thrown by the core API.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
public class SourceAPIError {

    private final List<String> errors = new ArrayList<>();

    public String[] getErrors() {
        return errors.toArray(new String[0]);
    }

    public SourceAPIError addError(@NotNull String error) {
        errors.add(error);
        return this;
    }
}
