package com.sourceplusplus.api.model.trace;

/**
 * Different trace order types core can be queried by.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.3.0
 * @since 0.1.0
 */
public enum TraceOrderType {

    LATEST_TRACES,
    SLOWEST_TRACES,
    FAILED_TRACES,
    TOTAL_TRACES
}
