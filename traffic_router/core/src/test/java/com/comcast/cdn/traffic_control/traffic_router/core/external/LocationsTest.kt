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
import org.hamcrest.MatcherAssert
import org.hamcrest.core.AnyOf
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(ExternalTest::class)
class LocationsTest {
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
    fun itGetsAListOfLocations() {
        val httpGet = HttpGet("http://localhost:3333/crs/locations")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            MatcherAssert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val objectMapper = ObjectMapper(JsonFactory())
            val jsonNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            MatcherAssert.assertThat(jsonNode["locations"][0].asText(), IsNot.not(IsEqual.equalTo("")))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itGetsAListOfCaches() {
        val httpGet = HttpGet("http://localhost:3333/crs/locations/caches")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            val jsonNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val locationName = jsonNode["locations"].fieldNames().next()
            val cacheNode = jsonNode["locations"][locationName][0]
            MatcherAssert.assertThat(cacheNode["cacheId"].asText(), IsNot.not(IsEqual.equalTo("")))
            MatcherAssert.assertThat(cacheNode["fqdn"].asText(), IsNot.not(IsEqual.equalTo("")))
            MatcherAssert.assertThat(cacheNode["ipAddresses"].isArray, IsEqual.equalTo(true))
            MatcherAssert.assertThat(cacheNode.has("adminStatus"), IsEqual.equalTo(true))
            MatcherAssert.assertThat(cacheNode["port"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["lastUpdateTime"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["connections"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["currentBW"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["availBW"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(
                cacheNode["cacheOnline"].asText(),
                AnyOf.anyOf(IsEqual.equalTo("true"), IsEqual.equalTo("false"))
            )
            MatcherAssert.assertThat(
                cacheNode["lastUpdateHealthy"].asText(),
                AnyOf.anyOf(IsEqual.equalTo("true"), IsEqual.equalTo("false"))
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itGetsCachesForALocation() {
        var httpGet = HttpGet("http://localhost:3333/crs/locations")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            var jsonNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val location = jsonNode["locations"][0].asText()
            httpGet = HttpGet("http://localhost:3333/crs/locations/$location/caches")
            response = closeableHttpClient!!.execute(httpGet)
            jsonNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            MatcherAssert.assertThat(jsonNode["caches"].isArray, IsEqual.equalTo(true))
            val cacheNode = jsonNode["caches"][0]
            MatcherAssert.assertThat(cacheNode["cacheId"].asText(), IsNot.not(IsEqual.equalTo("")))
            MatcherAssert.assertThat(cacheNode["fqdn"].asText(), IsNot.not(IsEqual.equalTo("")))
            MatcherAssert.assertThat(cacheNode["ipAddresses"].isArray, IsEqual.equalTo(true))
            MatcherAssert.assertThat(cacheNode.has("adminStatus"), IsEqual.equalTo(true))
            MatcherAssert.assertThat(cacheNode["port"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["lastUpdateTime"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["connections"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["currentBW"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(cacheNode["availBW"].asInt(-123456), IsNot.not(IsEqual.equalTo(-123456)))
            MatcherAssert.assertThat(
                cacheNode["cacheOnline"].asText(),
                AnyOf.anyOf(IsEqual.equalTo("true"), IsEqual.equalTo("false"))
            )
            MatcherAssert.assertThat(
                cacheNode["lastUpdateHealthy"].asText(),
                AnyOf.anyOf(IsEqual.equalTo("true"), IsEqual.equalTo("false"))
            )
        } finally {
            response?.close()
        }
    }
}