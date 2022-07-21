package com.valensas.ratelimiter.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("rate-limiter")
data class RateLimiterConfig(
    val limiters: List<RateLimiter>,
    val costs: List<Cost>
) {
    data class RateLimiter(
        val capacity: Long,
        val period: Duration,
        val name: String
    )
    data class Cost(
        val path: String,
        val cost: Long
    )
}
