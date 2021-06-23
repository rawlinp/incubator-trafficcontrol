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
package com.comcast.cdn.traffic_control.traffic_router.core.edge

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryServiceMatcher
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.request.Request
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class CacheRegisterTest {
    private val cacheRegister = CacheRegister()

    @Before
    fun before() {
        val deliveryService1 = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService1.id).thenReturn("delivery service 1")
        val deliveryService2 = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService2.id).thenReturn("delivery service 2")
        val deliveryServiceMatcher1 = DeliveryServiceMatcher(deliveryService1)
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown\\..*", "")
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abc.*", "")
        val deliveryServiceMatcher2 = DeliveryServiceMatcher(deliveryService2)
        deliveryServiceMatcher2.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown\\..*", "")
        deliveryServiceMatcher2.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abcde.*", "")
        val deliveryServiceMatcher3 = DeliveryServiceMatcher(deliveryService2)
        deliveryServiceMatcher3.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown\\..*", "")
        deliveryServiceMatcher3.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abcd.*", "")
        val deliveryServiceMatchers = TreeSet<DeliveryServiceMatcher>()
        deliveryServiceMatchers.add(deliveryServiceMatcher1)
        deliveryServiceMatchers.add(deliveryServiceMatcher2)
        deliveryServiceMatchers.add(deliveryServiceMatcher3)
        val dnsMatcher1 = DeliveryServiceMatcher(deliveryService1)
        dnsMatcher1.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown\\..*", "")
        val dnsMatchers = TreeSet<DeliveryServiceMatcher>()
        deliveryServiceMatchers.add(dnsMatcher1)
        cacheRegister.setDeliveryServiceMatchers(deliveryServiceMatchers)
    }

    @Test
    fun itPicksTheMostSpecificDeliveryService() {
        val httpRequest = HTTPRequest()
        httpRequest.hostname = "foo.service01-kabletown.com"
        httpRequest.path = "foo/abcde/bar"
        MatcherAssert.assertThat(
            cacheRegister.getDeliveryService(httpRequest).id,
            Matchers.equalTo("delivery service 2")
        )
        val request = Request()
        request.hostname = "foo.service01-kabletown.com"
        MatcherAssert.assertThat(cacheRegister.getDeliveryService(request).id, Matchers.equalTo("delivery service 1"))
    }

    @Test
    fun itReturnsNullForDeliveryServiceWhenItHasNoMatchers() {
        cacheRegister.setDeliveryServiceMatchers(null)
        val httpRequest = HTTPRequest()
        httpRequest.hostname = "foo.service01-kabletown.com"
        httpRequest.path = "foo/abcde/bar"
        MatcherAssert.assertThat(cacheRegister.getDeliveryService(httpRequest), Matchers.nullValue())
    }
}