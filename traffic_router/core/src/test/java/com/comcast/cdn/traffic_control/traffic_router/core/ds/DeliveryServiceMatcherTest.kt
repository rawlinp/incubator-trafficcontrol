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
package com.comcast.cdn.traffic_control.traffic_router.core.ds

import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.request.Request
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@PrepareForTest(DeliveryService::class)
@RunWith(PowerMockRunner::class)
class DeliveryServiceMatcherTest {
    @Test
    fun itReturnsTrueWhenAllMatchersPass() {
        val deliveryServiceMatcher = DeliveryServiceMatcher(
            Mockito.mock(
                DeliveryService::class.java
            )
        )
        deliveryServiceMatcher.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abcd.*", "")
        val httpRequest = HTTPRequest()
        httpRequest.hostname = "foo.service01-kabletown.com.bar"
        httpRequest.path = "foo/abcd/bar"
        Assert.assertThat(deliveryServiceMatcher.matches(httpRequest), Matchers.equalTo(true))
    }

    @Test
    fun itReturnsFalseWhenAnyMatcherFails() {
        val deliveryServiceMatcher = DeliveryServiceMatcher(
            Mockito.mock(
                DeliveryService::class.java
            )
        )
        deliveryServiceMatcher.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abcd.*", "")
        val httpRequest = HTTPRequest()
        httpRequest.hostname = "foo.serviceZZ-kabletown.com.bar"
        httpRequest.path = "foo/abcd/bar"
        Assert.assertThat(deliveryServiceMatcher.matches(httpRequest), Matchers.equalTo(false))
    }

    @Test
    fun itReturnsFalseWhenItHasNoMatchers() {
        val deliveryServiceMatcher = DeliveryServiceMatcher(
            Mockito.mock(
                DeliveryService::class.java
            )
        )
        val request = Request()
        Assert.assertThat(deliveryServiceMatcher.matches(request), Matchers.equalTo(false))
        val httpRequest = HTTPRequest()
        Assert.assertThat(deliveryServiceMatcher.matches(httpRequest), Matchers.equalTo(false))
    }

    @Test
    fun itComparesByMatchRegexes() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        val deliveryServiceMatcher1 = DeliveryServiceMatcher(deliveryService)
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abc.*", "")
        val deliveryServiceMatcher1a = DeliveryServiceMatcher(deliveryService)
        deliveryServiceMatcher1a.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher1a.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abc.*", "")
        val deliveryServiceMatcher2 = DeliveryServiceMatcher(deliveryService)
        deliveryServiceMatcher2.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher2.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abcde.*", "")
        Assert.assertThat(deliveryServiceMatcher1 == deliveryServiceMatcher1a, Matchers.equalTo(true))
        Assert.assertThat(deliveryServiceMatcher1a == deliveryServiceMatcher1, Matchers.equalTo(true))
        Assert.assertThat(deliveryServiceMatcher1.compareTo(deliveryServiceMatcher1), Matchers.equalTo(0))
        Assert.assertThat(deliveryServiceMatcher1.compareTo(deliveryServiceMatcher1a), Matchers.equalTo(0))
        Assert.assertThat(deliveryServiceMatcher1.compareTo(deliveryServiceMatcher2), Matchers.greaterThan(0))
        Assert.assertThat(deliveryServiceMatcher2.compareTo(deliveryServiceMatcher1), Matchers.lessThan(0))
    }

    @Test
    fun itHandlesMatcherWithoutRequestMatchers() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        val deliveryServiceMatcher1 = DeliveryServiceMatcher(deliveryService)
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abc.*", "")
        val deliveryServiceMatcher2 = DeliveryServiceMatcher(deliveryService)
        Assert.assertThat(deliveryServiceMatcher1.compareTo(deliveryServiceMatcher2), Matchers.equalTo(-1))
        Assert.assertThat(deliveryServiceMatcher2.compareTo(deliveryServiceMatcher1), Matchers.equalTo(1))
    }

    @Test
    fun compareToReturns0WhenSameMatchersDifferentDeliveryServices() {
        val deliveryService1 = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService1.id).thenReturn("delivery service 1")
        val deliveryService2 = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService2.id).thenReturn("delivery service 2")
        val deliveryServiceMatcher1 = DeliveryServiceMatcher(deliveryService1)
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher1.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abc.*", "")
        val deliveryServiceMatcher2 = DeliveryServiceMatcher(deliveryService2)
        deliveryServiceMatcher2.addMatch(DeliveryServiceMatcher.Type.HOST, ".*\\.service01-kabletown.com\\..*", "")
        deliveryServiceMatcher2.addMatch(DeliveryServiceMatcher.Type.PATH, ".*abc.*", "")
        Assert.assertThat(deliveryServiceMatcher1 == deliveryServiceMatcher2, Matchers.equalTo(false))
        Assert.assertThat(deliveryServiceMatcher2 == deliveryServiceMatcher1, Matchers.equalTo(false))
        Assert.assertThat(deliveryServiceMatcher1.compareTo(deliveryServiceMatcher2), Matchers.equalTo(0))
    }
}