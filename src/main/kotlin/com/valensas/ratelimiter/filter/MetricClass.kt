package com.valensas.ratelimiter.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
object MetricClass {

    private lateinit var failCounter: Counter
    @Autowired
    fun setCounter(meterRegistry: MeterRegistry) {
        failCounter = meterRegistry.counter("rate_limiter_fail_request_counter")
    }

    fun processRequest() {
        failCounter.increment()

    }
}






