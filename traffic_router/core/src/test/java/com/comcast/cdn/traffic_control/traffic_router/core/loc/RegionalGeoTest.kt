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
package com.comcast.cdn.traffic_control.traffic_router.core.loc

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoResult.RegionalGeoResultType
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.HTTPRouteResult
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import com.comcast.cdn.traffic_control.traffic_router.geolocation.GeolocationException
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class RegionalGeoTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val dbFile = File("src/test/resources/regional_geoblock.json")
        RegionalGeo.parseConfigFile(dbFile, false)
    }

    @Test
    fun testEnforceAllowedCoordinateRange() {
        val dsvcId = "ds-geoblock-exclude"
        val url = "http://ds1.example.com/live1"
        val postal: String? = null
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 12.0, 55.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALLOWED))
        MatcherAssert.assertThat(result.url, Matchers.equalTo(url))
    }

    @Test
    fun testEnforceAlternateWithCacheNoCoordinateRangeNoPostalCode() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds2.example.com/live2"
        val postal: String? = null
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 12.0, 55.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITH_CACHE))
    }

    @Test
    fun testEnforceAllowed() {
        val dsvcId = "ds-geoblock-exclude"
        val url = "http://ds1.example.com/live1"
        val postal = "N7G"
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALLOWED))
        MatcherAssert.assertThat(result.url, Matchers.equalTo(url))
    }

    @Test
    fun testEnforceAlternateWithCache() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds2.example.com/live2"
        val postal = "N7G"
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITH_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("/path/redirect_T2"))
    }

    @Test
    fun testEnforceAlternateWithoutCache() {
        val dsvcId = "ds-geoblock-exclude"
        val url = "http://ds1.example.com/live1"
        val postal = "V5G"
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITHOUT_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("http://example.com/redirect_T1"))
    }

    @Test
    fun testEnforceDeniedNoDsvc() {
        val dsvcId = "ds-geoblock-no-exist"
        val url = "http://ds1.example.com/live1"
        val postal = "V5G"
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.DENIED))
    }

    @Test
    fun testEnforceDeniedNoRegexMatch() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds1.example.com/live-not-exist"
        val postal = "V5G"
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.DENIED))
    }

    @Test
    fun testEnforceAlternateToPathNoSlash() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds1.example.com/live3"
        val postal = "V5D"
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITH_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("/redirect_T3"))
    }

    @Test
    fun testEnforceAlternateNullPostal() {
        val dsvcId = "ds-geoblock-exclude"
        val url = "http://ds1.example.com/live1"
        val postal: String? = null
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITHOUT_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("http://example.com/redirect_T1"))
    }

    @Test
    fun testEnforceAlternateEmptyPostalInclude() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds2.example.com/live2"
        val postal = ""
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITH_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("/path/redirect_T2"))
    }

    @Test
    fun testEnforceAlternateEmptyPostalExclude() {
        val dsvcId = "ds-geoblock-exclude"
        val url = "http://ds1.example.com/live1"
        val postal = ""
        val ip = "10.0.0.1"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITHOUT_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("http://example.com/redirect_T1"))
    }

    @Test
    fun testEnforceWhiteListAllowed() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds1.example.com/live4"
        val postal: String? = null
        val ip = "129.100.254.2"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALLOWED))
        MatcherAssert.assertThat(result.url, Matchers.equalTo(url))
    }

    @Test
    fun testEnforceAllowedHttpsRedirect() {
        val dsvcId = "ds-geoblock-redirect-https"
        val url = "http://ds1.example.com/httpsredirect"
        val postal: String? = null
        val ip = "129.100.254.2"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITHOUT_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("https://example.com/redirect_https"))
    }

    @Test
    fun testEnforceSteeringReDirect() {
        val dsvcId = "ds-steering-1"
        val url = "http://ds1.example.com/steering"
        val postal: String? = null
        val ip = "129.100.254.4"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITHOUT_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("https://example.com/steering-test"))
    }

    @Test
    fun testEnforceNotInWhiteListAlternate() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds1.example.com/live4"
        val postal = "N7G"
        val ip = "129.202.254.2"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALTERNATE_WITH_CACHE))
        MatcherAssert.assertThat(result.url, Matchers.equalTo("/redirect_T4"))
    }

    @Test
    fun testEnforceNotInWhiteListAllowedByPostal() {
        val dsvcId = "ds-geoblock-include"
        val url = "http://ds1.example.com/live4"
        val postal = "N6G"
        val ip = "129.202.254.2"
        val result = RegionalGeo.enforce(dsvcId, url, ip, postal, 0.0, 0.0)
        MatcherAssert.assertThat(result.type, Matchers.equalTo(RegionalGeoResultType.ALLOWED))
        MatcherAssert.assertThat(result.url, Matchers.equalTo(url))
    }

    @Test
    @Throws(GeolocationException::class, MalformedURLException::class)
    fun testEnforceWhiteListAllowedRouteResultMultipleUrls() {
        val clientIp = "129.100.254.2"
        val requestUrl = "http://ds1.example.com/live4"
        val request = HTTPRequest()
        request.clientIP = clientIp
        request.hostname = "ds1.example.com"
        request.applyUrl(URL(requestUrl))
        val track = StatTracker.Track()
        val cache = PowerMockito.mock(
            Cache::class.java
        )
        val ds = PowerMockito.mock(DeliveryService::class.java)
        PowerMockito.`when`(ds.id).thenReturn("ds-geoblock-include")
        PowerMockito.`when`(ds.createURIString(request, cache)).thenReturn(requestUrl)
        val tr = PowerMockito.mock(TrafficRouter::class.java)
        PowerMockito.`when`(tr.getClientGeolocation(clientIp, track, ds)).thenReturn(Geolocation(42.0, -71.0))
        val routeResult = HTTPRouteResult(true)
        val firstUrl = "http://example.com/url1.m3u8"
        routeResult.addUrl(URL(firstUrl))
        RegionalGeo.enforce(tr, request, ds, cache, routeResult, track)
        MatcherAssert.assertThat(routeResult.urls.size, Matchers.equalTo(2))
        MatcherAssert.assertThat(routeResult.urls[1].toString(), Matchers.equalTo(requestUrl))
        MatcherAssert.assertThat(routeResult.urls[0].toString(), Matchers.equalTo(firstUrl))
    }
}