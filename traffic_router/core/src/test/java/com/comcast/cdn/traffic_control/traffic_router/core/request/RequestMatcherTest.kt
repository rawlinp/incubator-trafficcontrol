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
package com.comcast.cdn.traffic_control.traffic_router.core.request

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryServiceMatcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import java.util.*

class RequestMatcherTest {
    @Test
    fun itDoesNotAllowHEADERmatchesWithoutHeaderName() {
        try {
            RequestMatcher(DeliveryServiceMatcher.Type.HEADER, ".*kabletown.*")
            Assert.fail("Should have thrown IllegalArgumentException")
        } catch (iae: IllegalArgumentException) {
            MatcherAssert.assertThat(
                iae.message,
                Matchers.equalTo("Request Header name must be supplied for type HEADER")
            )
        }
        try {
            RequestMatcher(DeliveryServiceMatcher.Type.HEADER, ".*kabletown.*", "")
            Assert.fail("Should have thrown IllegalArgumentException")
        } catch (iae: IllegalArgumentException) {
            MatcherAssert.assertThat(
                iae.message,
                Matchers.equalTo("Request Header name must be supplied for type HEADER")
            )
        }
    }

    @Test
    fun itMatchesByHost() {
        val request = Request()
        val requestMatcher = RequestMatcher(DeliveryServiceMatcher.Type.HOST, ".*\\.host\\..*", "")
        MatcherAssert.assertThat(requestMatcher.matches(request), Matchers.equalTo(false))
        request.hostname = "foo.host.bar"
        MatcherAssert.assertThat(requestMatcher.matches(request), Matchers.equalTo(true))
    }

    @Test
    fun itMatchesByPath() {
        val requestMatcher = RequestMatcher(DeliveryServiceMatcher.Type.PATH, ".*path.*")
        val request = Request()
        MatcherAssert.assertThat(requestMatcher.matches(request), Matchers.equalTo(false))
        val httpRequest = HTTPRequest()
        httpRequest.path = "/foo/path/bar"
        MatcherAssert.assertThat(requestMatcher.matches(httpRequest), Matchers.equalTo(true))
    }

    @Test
    fun itMatchesByQuery() {
        val requestMatcher = RequestMatcher(DeliveryServiceMatcher.Type.PATH, ".*car=red.*")
        val request = Request()
        MatcherAssert.assertThat(requestMatcher.matches(request), Matchers.equalTo(false))
        val httpRequest = HTTPRequest()
        httpRequest.path = "/foo/path/bar"
        httpRequest.queryString = "car=red"
        MatcherAssert.assertThat(requestMatcher.matches(httpRequest), Matchers.equalTo(true))
    }

    @Test
    fun itMatchesByPathAndQuery() {
        val requestMatcher = RequestMatcher(DeliveryServiceMatcher.Type.PATH, "\\/foo\\/path\\/bar\\?car=red")
        val request = Request()
        MatcherAssert.assertThat(requestMatcher.matches(request), Matchers.equalTo(false))
        val httpRequest = HTTPRequest()
        httpRequest.path = "/foo/path/bar"
        httpRequest.queryString = "car=red"
        MatcherAssert.assertThat(requestMatcher.matches(httpRequest), Matchers.equalTo(true))
    }

    @Test
    fun itMatchesByRequestHeader() {
        val requestMatcher = RequestMatcher(DeliveryServiceMatcher.Type.HEADER, ".*kabletown.*", "Host")
        val request = Request()
        MatcherAssert.assertThat(requestMatcher.matches(request), Matchers.equalTo(false))
        val headers: MutableMap<String, String> = HashMap()
        headers["Host"] = "www.kabletown.com"
        val httpRequest = HTTPRequest()
        httpRequest.headers = headers
        MatcherAssert.assertThat(requestMatcher.matches(httpRequest), Matchers.equalTo(true))
    }

    @Test
    fun itSupportsOrderingByItsRegex() {
        val requestMatcher1 = RequestMatcher(DeliveryServiceMatcher.Type.PATH, ".*abcd.*")
        val requestMatcher2 = RequestMatcher(DeliveryServiceMatcher.Type.PATH, ".*abcde.*")
        val requestMatcher3 = RequestMatcher(DeliveryServiceMatcher.Type.PATH, ".*bcd.*")
        val requestMatcher4 = RequestMatcher(DeliveryServiceMatcher.Type.PATH, ".*bcdef.*")
        val set: MutableSet<RequestMatcher> = TreeSet()
        set.add(requestMatcher1)
        set.add(requestMatcher2)
        set.add(requestMatcher3)
        set.add(requestMatcher4)
        val iterator: Iterator<RequestMatcher> = set.iterator()
        MatcherAssert.assertThat(iterator.next(), Matchers.equalTo(requestMatcher2))
        MatcherAssert.assertThat(iterator.next(), Matchers.equalTo(requestMatcher4))
        MatcherAssert.assertThat(iterator.next(), Matchers.equalTo(requestMatcher1))
        MatcherAssert.assertThat(iterator.next(), Matchers.equalTo(requestMatcher3))
    }

    @Test
    fun itThrowsIllegalArgumentException() {
        try {
            RequestMatcher(DeliveryServiceMatcher.Type.HEADER, "a-regex")
            Assert.fail("Should have caught Illegal Argument Exception!")
        } catch (e: IllegalArgumentException) {
            MatcherAssert.assertThat(
                e.message,
                Matchers.equalTo("Request Header name must be supplied for type HEADER")
            )
        }
    }

    @Test
    fun itSupportsEquals() {
        val requestMatcher1 = RequestMatcher(DeliveryServiceMatcher.Type.HOST, ".*abc.*")
        val requestMatcher2 = RequestMatcher(DeliveryServiceMatcher.Type.HOST, ".*abc.*")
        MatcherAssert.assertThat(requestMatcher1, Matchers.equalTo(requestMatcher2))
        MatcherAssert.assertThat(requestMatcher2, Matchers.equalTo(requestMatcher1))
    }

    @Test
    fun itSupportsHashCode() {
        val requestMatcher1 = RequestMatcher(DeliveryServiceMatcher.Type.HOST, ".*abc.*")
        val requestMatcher2 = RequestMatcher(DeliveryServiceMatcher.Type.HOST, ".*abc.*")
        MatcherAssert.assertThat(requestMatcher1.hashCode(), Matchers.equalTo(requestMatcher2.hashCode()))
    }
}