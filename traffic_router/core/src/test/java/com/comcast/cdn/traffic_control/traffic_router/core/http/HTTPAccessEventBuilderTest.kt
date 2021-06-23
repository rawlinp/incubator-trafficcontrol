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
package com.comcast.cdn.traffic_control.traffic_router.core.http

import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.net.URL
import java.util.*
import javax.servlet.http.HttpServletRequest

@RunWith(PowerMockRunner::class)
@PrepareForTest(Date::class, HTTPAccessEventBuilder::class, HTTPAccessRecord::class, System::class)
class HTTPAccessEventBuilderTest {
    private var request: HttpServletRequest? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        PowerMockito.mockStatic(Date::class.java)
        val startDate = Mockito.mock(Date::class.java)
        Mockito.`when`(startDate.time).thenReturn(144140678000L)
        PowerMockito.whenNew(Date::class.java).withArguments(Matchers.anyLong()).thenReturn(startDate)
        val finishDate = Mockito.mock(Date::class.java)
        Mockito.`when`(finishDate.time).thenReturn(144140678125L)
        PowerMockito.whenNew(Date::class.java).withNoArguments().thenReturn(finishDate)
        request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request!!.getRequestURL()).thenReturn(StringBuffer("http://example.com/index.html?foo=bar"))
        Mockito.`when`(request!!.getMethod()).thenReturn("GET")
        Mockito.`when`(request!!.getProtocol()).thenReturn("HTTP/1.1")
        Mockito.`when`(request!!.getRemoteAddr()).thenReturn("192.168.7.6")
        PowerMockito.mockStatic(System::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun itGeneratesAccessEvents() {
        val builder = HTTPAccessRecord.Builder(Date(144140678000L), request)
        val httpAccessRecord = builder.build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.7.6 rhi=- url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=- rloc=\"-\" rdtl=- rerr=\"-\" rgb=\"-\" rurl=\"-\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itAddsResponseData() {
        Mockito.`when`(System.nanoTime()).thenReturn(100111001L, 225111001L)
        val track = StatTracker.Track()
        val builder = HTTPAccessRecord.Builder(Date(144140633999L), request)
            .resultType(track.result)
            .resultLocation(Geolocation(39.7528, -104.9997))
            .responseCode(302)
            .responseURL(URL("http://example.com/hereitis/index.html?foo=bar"))
        val httpAccessRecord = builder.resultType(ResultType.CZ).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.7.6 rhi=- url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=CZ rloc=\"39.75,-104.99\" rdtl=- rerr=\"-\" rgb=\"-\" pssc=302 ttms=125.000 rurl=\"http://example.com/hereitis/index.html?foo=bar\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itAddsMuiltiResponseData() {
        Mockito.`when`(System.nanoTime()).thenReturn(100111001L, 225111001L)
        val urls: MutableList<URL> = ArrayList()
        urls.add(URL("http://example.com/hereitis/index.html?foo=bar"))
        urls.add(URL("http://example.com/thereitis/index.html?boo=baz"))
        val track = StatTracker.Track()
        val builder = HTTPAccessRecord.Builder(Date(144140633999L), request)
            .resultType(track.result)
            .resultLocation(Geolocation(39.7528, -104.9997))
            .responseCode(302)
            .responseURL(URL("http://example.com/hereitis/index.html?foo=bar"))
            .responseURLs(urls)
        val httpAccessRecord = builder.resultType(ResultType.CZ).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.7.6 rhi=- url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=CZ rloc=\"39.75,-104.99\" rdtl=- rerr=\"-\" rgb=\"-\" pssc=302 ttms=125.000 rurl=\"http://example.com/hereitis/index.html?foo=bar\" rurls=\"[http://example.com/hereitis/index.html?foo=bar, http://example.com/thereitis/index.html?boo=baz]\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itRoundsUpToNearestMicroSecond() {
        Mockito.`when`(System.nanoTime()).thenReturn(100111001L, 100234999L)
        val fastFinishDate = Mockito.mock(Date::class.java)
        Mockito.`when`(fastFinishDate.time).thenReturn(144140678000L)
        PowerMockito.whenNew(Date::class.java).withNoArguments().thenReturn(fastFinishDate)
        val track = StatTracker.Track()
        val builder = HTTPAccessRecord.Builder(Date(144140633999L), request)
            .resultType(track.result)
            .responseCode(302)
            .responseURL(URL("http://example.com/hereitis/index.html?foo=bar"))
        val httpAccessRecord = builder.build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.7.6 rhi=- url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=ERROR rloc=\"-\" rdtl=- rerr=\"-\" rgb=\"-\" pssc=302 ttms=0.124 rurl=\"http://example.com/hereitis/index.html?foo=bar\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itRecordsTrafficRouterErrors() {
        Mockito.`when`(System.nanoTime()).thenReturn(111001L, 567002L)
        val fastFinishDate = Mockito.mock(Date::class.java)
        Mockito.`when`(fastFinishDate.time).thenReturn(144140678000L)
        PowerMockito.whenNew(Date::class.java).withNoArguments().thenReturn(fastFinishDate)
        val track = StatTracker.Track()
        val builder = HTTPAccessRecord.Builder(Date(144140633999L), request)
            .resultType(track.result)
            .responseCode(302)
            .rerr("RuntimeException: you're doing it wrong")
            .responseURL(URL("http://example.com/hereitis/index.html?foo=bar"))
        val httpAccessRecord = builder.build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.7.6 rhi=- url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=ERROR rloc=\"-\" rdtl=- rerr=\"RuntimeException: you're doing it wrong\" rgb=\"-\" pssc=302 ttms=0.456 rurl=\"http://example.com/hereitis/index.html?foo=bar\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itRecordsMissResultDetails() {
        Mockito.`when`(System.nanoTime()).thenReturn(100000101L, 100789000L)
        val fastFinishDate = Mockito.mock(Date::class.java)
        Mockito.`when`(fastFinishDate.time).thenReturn(144140678000L)
        PowerMockito.whenNew(Date::class.java).withNoArguments().thenReturn(fastFinishDate)
        val builder = HTTPAccessRecord.Builder(Date(144140633999L), request)
            .resultType(ResultType.MISS)
            .resultDetails(ResultDetails.DS_NO_BYPASS)
            .responseCode(503)
        val httpAccessRecord = builder.build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.7.6 rhi=- url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=MISS rloc=\"-\" rdtl=DS_NO_BYPASS rerr=\"-\" rgb=\"-\" pssc=503 ttms=0.789 rurl=\"-\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itRecordsRequestHeaders() {
        val httpAccessRequestHeaders: MutableMap<String, String> = HashMap()
        httpAccessRequestHeaders["If-Modified-Since"] = "Thurs, 15 July 2010 12:00:00 UTC"
        httpAccessRequestHeaders["Accept"] = "text/*, text/html, text/html;level=1, */*"
        httpAccessRequestHeaders["Arbitrary"] = "The cow says \"moo\""
        val track = StatTracker.Track()
        val builder = HTTPAccessRecord.Builder(Date(144140633999L), request)
            .resultType(track.result)
            .resultLocation(Geolocation(39.7528, -104.9997))
            .responseCode(302)
            .responseURL(URL("http://example.com/hereitis/index.html?foo=bar"))
            .requestHeaders(httpAccessRequestHeaders)
        val httpAccessRecord = builder.resultType(ResultType.CZ).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(" rh=\"-\""))
        )
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.containsString("rh=\"If-Modified-Since: Thurs, 15 July 2010 12:00:00 UTC\"")
        )
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.containsString("rh=\"Accept: text/*, text/html, text/html;level=1, */*\"")
        )
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.containsString("rh=\"Arbitrary: The cow says 'moo'")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itUsesXMmClientIpHeaderForChi() {
        Mockito.`when`(request!!.getHeader(HTTPRequest.X_MM_CLIENT_IP)).thenReturn("192.168.100.100")
        Mockito.`when`(request!!.remoteAddr).thenReturn("12.34.56.78")
        val httpAccessRecord = HTTPAccessRecord.Builder(Date(144140678000L), request).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.100.100 rhi=12.34.56.78 url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=- rloc=\"-\" rdtl=- rerr=\"-\" rgb=\"-\" rurl=\"-\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itUsesFakeIpParameterForChi() {
        Mockito.`when`(request!!.getParameter("fakeClientIpAddress")).thenReturn("192.168.123.123")
        val httpAccessRecord = HTTPAccessRecord.Builder(Date(144140678000L), request).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.123.123 rhi=192.168.7.6 url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=- rloc=\"-\" rdtl=- rerr=\"-\" rgb=\"-\" rurl=\"-\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itUsesXMmClientIpHeaderOverFakeIpParameterForChi() {
        Mockito.`when`(request!!.getParameter("fakeClientIpAddress")).thenReturn("192.168.123.123")
        Mockito.`when`(request!!.getHeader(HTTPRequest.X_MM_CLIENT_IP)).thenReturn("192.168.100.100")
        val httpAccessRecord = HTTPAccessRecord.Builder(Date(144140678000L), request).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.equalTo("144140678.000 qtype=HTTP chi=192.168.100.100 rhi=192.168.7.6 url=\"http://example.com/index.html?foo=bar\" cqhm=GET cqhv=HTTP/1.1 rtype=- rloc=\"-\" rdtl=- rerr=\"-\" rgb=\"-\" rurl=\"-\" rurls=\"-\" uas=\"null\" rh=\"-\"")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itUsesUserAgentHeaderString() {
        Mockito.`when`(request!!.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Gecko/20100101 Firefox/68.0")
        val httpAccessRecord = HTTPAccessRecord.Builder(Date(144140678000L), request).build()
        val httpAccessEvent = HTTPAccessEventBuilder.create(httpAccessRecord)
        MatcherAssert.assertThat(
            httpAccessEvent,
            org.hamcrest.Matchers.containsString("uas=\"Mozilla/5.0 Gecko/20100101 Firefox/68.0\"")
        )
    }
}