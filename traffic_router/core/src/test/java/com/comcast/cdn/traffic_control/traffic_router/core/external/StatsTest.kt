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
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.LifecycleException
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.*

@Category(ExternalTest::class)
class StatsTest {
    var httpClient: CloseableHttpClient? = null

    @Before
    @Throws(LifecycleException::class)
    fun before() {
        httpClient = HttpClientBuilder.create().build()
    }

    @After
    @Throws(Exception::class)
    fun after() {
        if (httpClient != null) httpClient!!.close()
    }

    @Test
    @Throws(Exception::class)
    fun itGetsApplicationStats() {
        val httpGet = HttpGet("http://localhost:3333/crs/stats")
        var httpResponse: CloseableHttpResponse? = null
        try {
            httpResponse = httpClient!!.execute(httpGet)
            val responseContent = EntityUtils.toString(httpResponse.entity)
            val objectMapper = ObjectMapper()
            val data: Map<String, Any> = objectMapper.readValue<HashMap<String, Any>>(
                responseContent,
                object : TypeReference<HashMap<String, Any>?>() {})
            MatcherAssert.assertThat(data.keys, Matchers.containsInAnyOrder("app", "stats"))
            val appData = data["app"] as Map<String, Any>?
            MatcherAssert.assertThat(
                appData!!.keys,
                Matchers.containsInAnyOrder("buildTimestamp", "name", "deploy-dir", "git-revision", "version")
            )
            val statsData = data["stats"] as Map<String, Any>?
            MatcherAssert.assertThat(
                statsData!!.keys,
                Matchers.containsInAnyOrder(
                    "dnsMap",
                    "httpMap",
                    "totalDnsCount",
                    "totalHttpCount",
                    "totalDsMissCount",
                    "appStartTime",
                    "averageDnsTime",
                    "averageHttpTime",
                    "updateTracker"
                )
            )
            val dnsStats = statsData["dnsMap"] as Map<String, Any>?
            val cacheDnsStats = dnsStats!!.values.iterator().next() as Map<String, Any>
            MatcherAssert.assertThat(
                cacheDnsStats.keys, Matchers.containsInAnyOrder(
                    "czCount", "geoCount", "missCount", "dsrCount", "errCount",
                    "deepCzCount", "staticRouteCount", "fedCount", "regionalDeniedCount", "regionalAlternateCount"
                )
            )
            val httpStats = statsData["httpMap"] as Map<String, Any>?
            val cacheHttpStats = httpStats!!.values.iterator().next() as Map<String, Any>
            MatcherAssert.assertThat(
                cacheHttpStats.keys, Matchers.containsInAnyOrder(
                    "czCount", "geoCount", "missCount", "dsrCount", "errCount",
                    "deepCzCount", "staticRouteCount", "fedCount", "regionalDeniedCount", "regionalAlternateCount"
                )
            )
            val updateTracker = statsData["updateTracker"] as Map<String, Any>?
            val keys = updateTracker!!.keys
            val expectedStats =
                Arrays.asList("lastCacheStateCheck", "lastCacheStateChange", "lastConfigCheck", "lastConfigChange")
            if (!keys.containsAll(expectedStats)) {
                val joiner = StringJoiner(",")
                for (stat in expectedStats) {
                    joiner.add(stat)
                }
                Assert.fail("Missing at least one of the following keys '$joiner'")
            }
        } finally {
            httpResponse?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itGetsLocationsByIp() {
        val httpGet = HttpGet("http://localhost:3333/crs/stats/ip/8.8.8.8")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            val actual = EntityUtils.toString(response.entity)
            val data: Map<String, Any> = ObjectMapper().readValue<HashMap<String, Any>>(
                actual,
                object : TypeReference<HashMap<String, Any>?>() {})
            MatcherAssert.assertThat(data["requestIp"], Matchers.equalTo("8.8.8.8"))
            MatcherAssert.assertThat(data["locationByFederation"], Matchers.equalTo("not found"))
            MatcherAssert.assertThat(data["locationByCoverageZone"], Matchers.equalTo("not found"))
            val locationByGeo = data["locationByGeo"] as Map<String, Any>?
            MatcherAssert.assertThat(
                locationByGeo!!.keys,
                Matchers.containsInAnyOrder("city", "countryCode", "latitude", "longitude", "postalCode", "countryName")
            )
        } finally {
            response?.close()
        }
    }
}