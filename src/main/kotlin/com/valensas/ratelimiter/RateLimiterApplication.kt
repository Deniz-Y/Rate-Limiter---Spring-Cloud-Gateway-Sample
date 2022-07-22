package com.valensas.ratelimiter

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
@ConfigurationPropertiesScan
class RateLimiterApplication

fun main(args: Array<String>) {
    runApplication<RateLimiterApplication>(*args)
}

@Bean
fun timedAspect(registry: MeterRegistry?): TimedAspect? {
    return TimedAspect(registry!!)
}
