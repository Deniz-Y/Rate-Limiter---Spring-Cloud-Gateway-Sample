package com.valensas.ratelimiter.filter


import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

@Service
class MetricService(
    private val registry: MeterRegistry
) {
    private val requestActionCounters = hashMapOf<String, Counter>()

    fun incrementRequestAction(
        path: String,
        ip: String = "0.0.0.0",
        action: RateLimiterAction
    ) {
        val key = path + ip + action.name
        val counter = requestActionCounters.computeIfAbsent(key) {
            registry.counter("rate_limiter_permitted_request", "path", path, "platform", ip, "action", action.name)
        }
        counter.increment()
    }
}

enum class RateLimiterAction {
    Block, Permit
}

