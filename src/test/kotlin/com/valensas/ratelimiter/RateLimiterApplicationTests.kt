package com.valensas.ratelimiter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.lang.Thread.sleep
import kotlin.math.roundToInt

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RateLimiterApplicationTests {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun contextLoads() {
        val limit = 10
        var intTime = 0

        for (it in limit downTo 0 step 3) {

            val response = restTemplate.exchange("/get", HttpMethod.GET, null, String::class.java)
            logger.info("Request: {} - Status: {}", it, response.statusCode)
            val expectedStatusCode = if (it > 3) HttpStatus.OK else HttpStatus.TOO_MANY_REQUESTS
            assertEquals(expectedStatusCode, response.statusCode)
            if (expectedStatusCode == HttpStatus.OK) {
                val remaining = response.headers["Remaining"]?.firstOrNull()
                logger.info("Remaining limit: {}", remaining)
                assertNotNull(remaining)
                assertEquals(it - 3, remaining!!.toInt())
            } else {
                val remainingTime = response.headers["Retry-After"]?.firstOrNull()
                if (remainingTime != null) {

                    intTime = remainingTime.toDouble().roundToInt()

                }


            }
        }
        var longTime = (intTime * 1000).toLong() + 1000
        logger.info("Sleep {} seconds", longTime)
        sleep(longTime)
        val response = restTemplate.exchange("/get", HttpMethod.GET, null, String::class.java)
        logger.info("Request after sleep - Status: {}", response.statusCode)
        assertEquals(HttpStatus.OK, response.statusCode)
    }


    @Test
    fun `apply ip based rate limit`() {

        val ip1 = "0:0:0:0:0:0:0:1"
        val ip2 = "0:0:0:0:0:0:0:2"
        val limit = 3

        // Send request from ip1 and get OK
        repeat(limit) { call(ip1, HttpStatus.OK) }
        // Get 429 if the rate limit is exceeded
        call(ip1, HttpStatus.TOO_MANY_REQUESTS)



        // Send request from ip2 and get OK
        repeat(limit) { call(ip2, HttpStatus.OK) }
        // Get 429 if the rate limit is exceeded
        call(ip2, HttpStatus.TOO_MANY_REQUESTS)

        // Send request from both ip addresses and get 429
        listOf(ip1, ip2).forEach { ip -> call(ip, HttpStatus.TOO_MANY_REQUESTS) }
    }

    fun call(ip: String, expectedStatus: HttpStatus) {
        logger.info("Sending request from {}. Expected response status: {}", ip, expectedStatus)
        val headers = HttpHeaders()
        headers.add("X-Forwarded-For", ip)

        val entity: HttpEntity<String> = HttpEntity(null, headers)

        val response = restTemplate.exchange("/get", HttpMethod.GET, entity, String::class.java)


        logger.info("Response status of the request from {} is {}", ip, response.statusCode)
        assertEquals(expectedStatus, response.statusCode)
    }

    @Test
    fun `endpoint test`() {

        //get endpoint uses 3 credits
        val responseForGetEndpoint = restTemplate.exchange("/get", HttpMethod.GET, null, String::class.java)
        assertEquals(HttpStatus.OK, responseForGetEndpoint.statusCode)

        //post endpoint uses 5 credits
        val responseForPostEndpoint = restTemplate.exchange("/post", HttpMethod.POST, null, String::class.java)
        assertEquals(HttpStatus.OK, responseForPostEndpoint.statusCode)

        val responseForGetEndpoint2 = restTemplate.exchange("/get", HttpMethod.GET, null, String::class.java)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseForGetEndpoint2.statusCode)

    }
}
