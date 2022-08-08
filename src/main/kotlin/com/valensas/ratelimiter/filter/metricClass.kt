package com.valensas.ratelimiter.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
@Service
object metricClass {

    private lateinit var failCounter: Counter
    @Autowired
    fun setCounter(meterRegistry: MeterRegistry) {
        failCounter = meterRegistry.counter("rate_limiter_fail_request_counter")
    }

    fun processRequest() {
        failCounter.increment()

    }
}






