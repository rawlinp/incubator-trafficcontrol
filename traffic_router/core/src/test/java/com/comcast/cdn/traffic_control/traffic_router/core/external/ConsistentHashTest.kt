/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.comcast.cdn.traffic_control.traffic_router.core.external

import com.comcast.cdn.traffic_control.traffic_router.core.util.CidrAddress
import com.comcast.cdn.traffic_control.traffic_router.core.util.ExternalTest
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtils
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.hamcrest.number.OrderingComparison
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.net.URLEncoder

@Category(ExternalTest::class)
class ConsistentHashTest {
    private var closeableHttpClient: CloseableHttpClient? = null
    var deliveryServiceId: String? = null
    var ipAddressInCoverageZone: String? = null
    var steeringDeliveryServiceId: String? = null
    var consistentHashRegex: String? = null
    var steeredDeliveryServices: MutableList<String> = ArrayList()

    @Before
    @Throws(Exception::class)
    fun before() {
        closeableHttpClient = HttpClientBuilder.create().build()
        var resourcePath = "api/2.0/steering"
        var inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        val objectMapper = ObjectMapper(JsonFactory())
        val steeringNode = objectMapper.readTree(inputStream)["response"][0]
        steeringDeliveryServiceId = steeringNode["deliveryService"].asText()
        val iterator: Iterator<JsonNode> = steeringNode["targets"].iterator()
        while (iterator.hasNext()) {
            val target = iterator.next()
            steeredDeliveryServices.add(target["deliveryService"].asText())
        }
        resourcePath = "publish/CrConfig.json"
        inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        var jsonNode = objectMapper.readTree(inputStream)
        deliveryServiceId = null
        val deliveryServices = jsonNode["deliveryServices"].fieldNames()
        while (deliveryServices.hasNext() && deliveryServiceId == null) {
            val dsId = deliveryServices.next()
            val deliveryServiceNode = jsonNode["deliveryServices"][dsId]
            if (deliveryServiceNode.has("steeredDeliveryServices")) {
                continue
            }
            val dispersionNode = deliveryServiceNode["dispersion"]
            if (dispersionNode == null || dispersionNode["limit"].asInt() != 1 && dispersionNode["shuffled"].asText() == "true") {
                continue
            }
            val matchsets: Iterator<JsonNode> = deliveryServiceNode["matchsets"].iterator()
            while (matchsets.hasNext() && deliveryServiceId == null) {
                if ("HTTP" == matchsets.next()["protocol"].asText()) {
                    if (deliveryServiceNode.has("consistentHashRegex")) {
                        deliveryServiceId = dsId
                        consistentHashRegex = deliveryServiceNode["consistentHashRegex"].asText()
                    }
                }
            }
            if (deliveryServiceId == null) {
                println("Skipping $deliveryServiceId no http protocol matchset")
            }
        }
        Assert.assertThat(deliveryServiceId, IsNot.not(CoreMatchers.nullValue()))
        Assert.assertThat(steeringDeliveryServiceId, IsNot.not(CoreMatchers.nullValue()))
        Assert.assertThat(steeredDeliveryServices.isEmpty(), IsEqual.equalTo(false))
        resourcePath = "czf.json"
        inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        jsonNode = objectMapper.readTree(inputStream)
        val network = jsonNode["coverageZones"][jsonNode["coverageZones"].fieldNames().next()]["network"]
        for (i in 0 until network.size()) {
            val cidrString = network[i].asText()
            val cidrAddress = CidrAddress.fromString(cidrString)
            if (cidrAddress.netmaskLength == 24) {
                val hostBytes = cidrAddress.hostBytes
                ipAddressInCoverageZone = String.format("%d.%d.%d.123", hostBytes[0], hostBytes[1], hostBytes[2])
                break
            }
        }
        Assert.assertThat(ipAddressInCoverageZone!!.length, OrderingComparison.greaterThan(0))
    }

    @After
    @Throws(Exception::class)
    fun after() {
        if (closeableHttpClient != null) closeableHttpClient!!.close()
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesConsistentHashingToRequestsForCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to find $ipAddressInCoverageZone in coverage zone using delivery service id $deliveryServiceId",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/another/different/path", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo(cacheId)))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo("")))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesConsistentHashingForRequestsOutsideCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/another/different/path", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo(cacheId)))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo("")))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesConsistentHashingToSteeringDeliveryService() {
        var response: CloseableHttpResponse? = null
        try {
            val requestPath = URLEncoder.encode("/some/path/thing", "UTF-8")
            val httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            val deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), Matchers.isIn(steeredDeliveryServices))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesBypassFiltersWithDeliveryServiceSteering() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/force-to-target-2/more/asdfasdf", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            var deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), IsEqual.equalTo("steering-target-2"))
            requestPath = URLEncoder.encode("/some/path/force-to-target-1/more/asdfasdf", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), IsEqual.equalTo("steering-target-1"))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesRegexToStandardizeRequestPath() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            val encodedConsistentHashRegex = URLEncoder.encode(consistentHashRegex, "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/patternbased/regex?regex=$encodedConsistentHashRegex&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to get 200 response from /consistenthash/patternbased/regex endpoint",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var resp = objectMapper.readTree(EntityUtils.toString(response.entity))
            val resultingPathToConsistentHash = resp["resultingPathToConsistentHash"].asText()
            requestPath = URLEncoder.encode("/other/path/other_thing.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/patternbased/regex?regex=$encodedConsistentHashRegex&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            resp = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(
                JsonUtils.optString(resp, "resultingPathToConsistentHash"),
                IsEqual.equalTo(resultingPathToConsistentHash)
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingToRequestsForCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to find $ipAddressInCoverageZone in coverage zone using delivery service id $deliveryServiceId",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/other/path/other_thing.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingForRequestsOutsideCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/other/path/other_thing.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingToSteeringDeliveryService() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            var deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val deliveryServiceId = deliveryServiceNode["id"].asText()
            Assert.assertThat(deliveryServiceId, Matchers.isIn(steeredDeliveryServices))
            response.close()
            requestPath =
                URLEncoder.encode("/other_different_path_12344321/path/other_thing_to_hash_differently.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), IsEqual.equalTo(deliveryServiceId))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingToSteeringRequestsForCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone/steering?ip=$ipAddressInCoverageZone&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to find $ipAddressInCoverageZone in coverage zone using delivery service id $deliveryServiceId",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath =
                URLEncoder.encode("/other_different_path_12344321/path/other_thing_to_hash_differently.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone/steering?ip=$ipAddressInCoverageZone&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
        } finally {
            response?.close()
        }
    }
}