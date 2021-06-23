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

import com.comcast.cdn.traffic_control.traffic_router.core.util.ExternalTest
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.LifecycleException
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.hamcrest.Matchers
import org.hamcrest.core.AnyOf
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(ExternalTest::class)
class CoverageZoneTest {
    private var closeableHttpClient: CloseableHttpClient? = null

    @Before
    @Throws(LifecycleException::class)
    fun before() {
        closeableHttpClient = HttpClientBuilder.create().build()
    }

    @After
    @Throws(Exception::class)
    fun after() {
        if (closeableHttpClient != null) closeableHttpClient!!.close()
    }

    @Test
    @Throws(Exception::class)
    fun itGetsCacheLocation() {
        val httpGet =
            HttpGet("http://localhost:3333/crs/coveragezone/cachelocation?ip=100.3.3.123&deliveryServiceId=steering-target-1")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            val jsonString = EntityUtils.toString(response.entity)
            val objectMapper = ObjectMapper(JsonFactory())
            val jsonNode = objectMapper.readTree(jsonString)
            Assert.assertThat(jsonNode["id"].asText(), IsEqual.equalTo("location-3"))
            Assert.assertThat(jsonNode["geolocation"], IsNot.not(Matchers.nullValue()))
            Assert.assertThat(jsonNode["caches"][0]["id"].asText(), Matchers.startsWith("edge-cache-03"))
            Assert.assertThat(jsonNode["caches"][0]["fqdn"].asText(), Matchers.startsWith("edge-cache-03"))
            Assert.assertThat(jsonNode["caches"][0]["fqdn"].asText(), Matchers.endsWith("thecdn.example.com"))
            Assert.assertThat(jsonNode["caches"][0]["port"].asInt(), Matchers.greaterThan(1024))
            Assert.assertThat(jsonNode["caches"][0]["hashValues"][0].asDouble(), Matchers.greaterThan(1.0))
            Assert.assertThat(isValidIpV4String(jsonNode["caches"][0]["ip4"].asText()), IsEqual.equalTo(true))
            Assert.assertThat(jsonNode["caches"][0]["ip6"].asText(), IsNot.not(IsEqual.equalTo("")))
            Assert.assertThat(jsonNode["caches"][0].has("available"), IsEqual.equalTo(true))
            Assert.assertThat(jsonNode.has("properties"), IsEqual.equalTo(true))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itGetsCaches() {
        val httpGet =
            HttpGet("http://localhost:3333/crs/coveragezone/caches?deliveryServiceId=steering-target-4&cacheLocationId=location-3")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val objectMapper = ObjectMapper(JsonFactory())
            val jsonNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(jsonNode.isArray, IsEqual.equalTo(true))
            val cacheNode = jsonNode[0]
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(Matchers.nullValue()))
            Assert.assertThat(cacheNode["fqdn"].asText(), IsNot.not(Matchers.nullValue()))
            Assert.assertThat(cacheNode["ip4"].asText(), IsNot.not(Matchers.nullValue()))
            Assert.assertThat(cacheNode["ip6"].asText(), IsNot.not(Matchers.nullValue()))
            // If the value is null or otherwise not an int we'll get back -123456, so any other value returned means success
            Assert.assertThat(cacheNode["port"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            Assert.assertThat(cacheNode["deliveryServices"].isArray, IsEqual.equalTo(true))
            Assert.assertThat(cacheNode["hashValues"][0].asDouble(-1024.1024), IsNot.not(IsEqual.equalTo(-1024.1024)))
            Assert.assertThat(
                cacheNode["available"].asText(),
                AnyOf.anyOf(IsEqual.equalTo("true"), IsEqual.equalTo("false"))
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itReturns404ForMissingDeliveryService() {
        val httpGet =
            HttpGet("http://localhost:3333/crs/coveragezone/caches?deliveryServiceId=ds-07&cacheLocationId=location-5")
        closeableHttpClient!!.execute(httpGet)
            .use { response -> Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(404)) }
    }

    fun isValidIpV4String(ip: String): Boolean {
        val octets = ip.split("\\.".toRegex()).toTypedArray()
        if (octets.size != 4) {
            return false
        }
        for (octet in octets) {
            val b = octet.toInt()
            if (b < 0 || 255 < b) {
                return false
            }
        }
        return true
    }
}