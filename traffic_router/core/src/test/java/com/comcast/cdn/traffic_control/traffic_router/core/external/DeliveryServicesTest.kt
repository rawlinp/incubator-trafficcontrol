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
import org.apache.catalina.LifecycleException
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.net.URLEncoder

@Category(ExternalTest::class)
class DeliveryServicesTest {
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
    fun itReturnsIdOfValidDeliveryService() {
        val encodedUrl = URLEncoder.encode("http://trafficrouter01.steering-target-1.thecdn.example.com/stuff", "utf-8")
        val httpGet = HttpGet("http://localhost:3333/crs/deliveryservices?url=$encodedUrl")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            val responseBody = EntityUtils.toString(response.entity)
            MatcherAssert.assertThat(responseBody, IsEqual.equalTo("{\"id\":\"steering-target-1\"}"))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsNotFoundForNonexistentDeliveryService() {
        val encodedUrl =
            URLEncoder.encode("http://trafficrouter01.somedeliveryservice.somecdn.domain.foo/stuff", "utf-8")
        val httpGet = HttpGet("http://localhost:3333/crs/deliveryservices?url=$encodedUrl")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            MatcherAssert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(404))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsBadRequestForBadUrlQueryParameter() {
        val encodedUrl = "httptrafficrouter01somedeliveryservicesomecdndomainfoo/stuff"
        val httpGet = HttpGet("http://localhost:3333/crs/deliveryservices?url=$encodedUrl")
        var response: CloseableHttpResponse? = null
        try {
            response = closeableHttpClient!!.execute(httpGet)
            MatcherAssert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(400))
        } finally {
            response?.close()
        }
    }
}