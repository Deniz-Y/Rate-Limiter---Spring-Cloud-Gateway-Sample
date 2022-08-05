package com.valensas.ratelimiter.filter

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager
import com.valensas.ratelimiter.config.RateLimiterConfig
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.regex.Pattern
import kotlin.math.pow

@Component
@ConditionalOnProperty(prefix = "rate-limit", name = ["enabled"], havingValue = "True")
class RateLimiterPreFilter(
    private val rateLimiterConfig: RateLimiterConfig,
    private val metricService: MetricService

) : GlobalFilter {
    val hzInstance: HazelcastInstance = Hazelcast.newHazelcastInstance()
    val map: IMap<String, ByteArray> = hzInstance.getMap("bucket-map")
    private val proxyManager: HazelcastProxyManager<String> = HazelcastProxyManager(map)

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        logger.info("Applying rate limiter filter")

        val BucketList = rateLimiterConfig.limiters.map {
            val configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(it.capacity, Refill.intervally(it.capacity, it.period)))
                .build()

          // logger.info("remote address: {}", exchange.request.remoteAddress?.address.toString())
            if(exchange.request.headers.containsKey("X-Forwarded-For")){
                val hazelcastBucket: Bucket = proxyManager.builder().build(exchange.request.headers.getValue("X-Forwarded-For").toString()+  it.name, configuration)
                hazelcastBucket

            }else{

                val hazelcastBucket: Bucket = proxyManager.builder().build(exchange.request.remoteAddress?.address.toString() +  it.name, configuration)
                hazelcastBucket
            }


        }

        val cost = rateLimiterConfig.costs.filter { it.path == exchange.request.path.value() }.sumOf { it.cost }
        val results = BucketList.map { it.tryConsumeAndReturnRemaining(cost) }
        logger.info("Results: {}", results)

        return if (results.all { it.isConsumed }) {
            exchange.response.headers.add("Remaining", results.minOf { it.remainingTokens }.toString())
            metricService.incrementRequestAction("","",RateLimiterAction.Block)
            metricClass.processRequest()
            chain.filter(exchange)
        } else {
            val waitTimeInSecond = results.maxOf { it.nanosToWaitForRefill } / 10.0.pow(9.0)
            exchange.response.headers.add("Retry-After", waitTimeInSecond.toString())
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.writeWith(Mono.empty())

        }
    }


}
