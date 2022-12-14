package com.valensas.ratelimiter.filter

import com.valensas.ratelimiter.config.RateLimiterConfig
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import kotlin.math.pow

@Component
class RateLimiterPreFilter(
    private val rateLimiterConfig: RateLimiterConfig
) : GlobalFilter {
    val buckets = rateLimiterConfig.limiters.map {
        Bucket.builder().addLimit(Bandwidth.simple(it.capacity, it.period)).build()
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        logger.info("Applying rate limiter filter")

        val cost = rateLimiterConfig.costs.filter { it.path == exchange.request.path.value() }.sumOf { it.cost }
        val results = buckets.map { it.tryConsumeAndReturnRemaining(cost) }
        logger.info("Results: {}", results)

        return if (results.all { it.isConsumed }) {
            exchange.response.headers.add("Remaining", results.minOf { it.remainingTokens }.toString())
            chain.filter(exchange)
        } else {
            val waitTimeInSecond = results.maxOf { it.nanosToWaitForRefill } / 10.0.pow(9.0)
            exchange.response.headers.add("Retry-After", waitTimeInSecond.toString())
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.writeWith(Mono.empty())
        }
    }
}
