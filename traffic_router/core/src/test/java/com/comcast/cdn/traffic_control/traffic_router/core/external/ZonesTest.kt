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
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(ExternalTest::class)
class ZonesTest {
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
    fun itGetsStatsForZones() {
        val httpGet = HttpGet("http://localhost:3333/crs/stats/zones/caches")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            val actual = EntityUtils.toString(response.entity)
            val zoneStats: Map<String, Any> = ObjectMapper().readValue<HashMap<String, Any>>(
                actual,
                object : TypeReference<HashMap<String, Any>?>() {})
            val dynamicZonesStats = zoneStats["dynamicZoneCaches"] as Map<String, Any>?
            MatcherAssert.assertThat(
                dynamicZonesStats!!.keys, Matchers.containsInAnyOrder(
                    "requestCount",
                    "evictionCount",
                    "totalLoadTime",
                    "averageLoadPenalty",
                    "hitCount",
                    "loadSuccessCount",
                    "missRate",
                    "loadExceptionRate",
                    "hitRate",
                    "missCount",
                    "loadCount",
                    "loadExceptionCount"
                )
            )
            val staticZonesStats = zoneStats["staticZoneCaches"] as Map<String, Any>?
            MatcherAssert.assertThat(
                staticZonesStats!!.keys, Matchers.containsInAnyOrder(
                    "requestCount",
                    "evictionCount",
                    "totalLoadTime",
                    "averageLoadPenalty",
                    "hitCount",
                    "loadSuccessCount",
                    "missRate",
                    "loadExceptionRate",
                    "hitRate",
                    "missCount",
                    "loadCount",
                    "loadExceptionCount"
                )
            )
        } finally {
            response?.close()
        }
    }
}