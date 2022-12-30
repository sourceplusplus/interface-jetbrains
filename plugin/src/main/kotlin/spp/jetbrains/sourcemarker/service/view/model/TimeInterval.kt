package spp.jetbrains.sourcemarker.service.view.model

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TimeInterval(
    var refreshInterval: Long,
    val keepSize: Int,
    val keepTimeSize: Long,
    val xStepSize: Long
) {
    LAST_5_MINUTES(500L, 10 * 6 * 5, 10_000L * 6 * 5, 10_000L * 6),
    LAST_15_MINUTES(1000L, 10 * 6 * 15, 10_000L * 6 * 15, 10_000L * 6 * 3),
    LAST_30_MINUTES(5000L, 10 * 6 * 30, 10_000L * 6 * 30, 10_000L * 6 * 6),
    LAST_1_HOUR(5000L, 10 * 6 * 60, 10_000L * 6 * 60, 10_000L * 6 * 12),
    LAST_4_HOURS(5000L, 10 * 6 * 60 * 4, 10_000L * 6 * 60 * 4, 10_000L * 6 * 60 * 2),
    LAST_12_HOURS(5000L, 10 * 6 * 60 * 12, 10_000L * 6 * 60 * 12, 10_000L * 6 * 60 * 6),
    LAST_24_HOURS(5000L, 10 * 6 * 60 * 24, 10_000L * 6 * 60 * 24, 10_000L * 6 * 60 * 12)
}
