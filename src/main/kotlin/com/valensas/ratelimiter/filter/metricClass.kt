package com.valensas.ratelimiter.filter
import io.prometheus.client.Counter

object metricClass {
       val requests = Counter.build()
            .name ("requests_total").help("Total requests.").register()

        fun processRequest() {
           requests.inc()

        }
    }

