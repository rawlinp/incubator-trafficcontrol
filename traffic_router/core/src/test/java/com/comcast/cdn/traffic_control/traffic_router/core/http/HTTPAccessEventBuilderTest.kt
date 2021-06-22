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

import kotlin.Throws
import java.lang.Exception
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringRegistry
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringTarget
import org.powermock.core.classloader.annotations.PrepareForTest
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryServiceMatcher
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringResult
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringGeolocationComparator
import org.junit.Before
import com.comcast.cdn.traffic_control.traffic_router.shared.ZoneTestRecords
import org.xbill.DNS.RRset
import com.comcast.cdn.traffic_control.traffic_router.core.dns.RRSetsBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.TCP
import java.net.InetAddress
import java.io.ByteArrayInputStream
import java.net.ServerSocket
import java.util.concurrent.BlockingQueue
import java.lang.Runnable
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.TCP.TCPSocketHandler
import org.xbill.DNS.DClass
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSAccessRecord
import org.xbill.DNS.Rcode
import java.lang.RuntimeException
import org.powermock.api.mockito.PowerMockito
import java.net.DatagramSocket
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.UDP
import org.xbill.DNS.OPTRecord
import java.util.concurrent.atomic.AtomicInteger
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.UDP.UDPPacketHandler
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.AbstractProtocolTest.FakeAbstractProtocol
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSAccessEventBuilder
import java.lang.System
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.AbstractProtocolTest
import java.net.Inet4Address
import org.xbill.DNS.ARecord
import org.xbill.DNS.WireParseException
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouterManager
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import org.xbill.DNS.NSRecord
import org.xbill.DNS.SOARecord
import org.xbill.DNS.ClientSubnetOption
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtils
import org.xbill.DNS.EDNSOption
import java.util.HashSet
import com.comcast.cdn.traffic_control.traffic_router.core.util.IntegrationTest
import java.util.HashMap
import com.comcast.cdn.traffic_control.traffic_router.core.dns.ZoneManagerTest
import com.google.common.net.InetAddresses
import org.xbill.DNS.TextParseException
import com.comcast.cdn.traffic_control.traffic_router.core.dns.ZoneManager
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node.IPVersions
import com.google.common.cache.CacheStats
import org.junit.BeforeClass
import java.nio.file.Paths
import com.comcast.cdn.traffic_control.traffic_router.core.TestBase
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSException
import com.comcast.cdn.traffic_control.traffic_router.core.dns.NameServerMain
import com.comcast.cdn.traffic_control.traffic_router.core.dns.ZoneSignerImpl
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DnsSecKeyPair
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DnsSecKeyPairImpl
import org.xbill.DNS.DNSKEYRecord
import java.security.PrivateKey
import org.xbill.DNS.RRSIGRecord
import com.comcast.cdn.traffic_control.traffic_router.core.dns.ZoneSignerImplTest.IsRRsetTypeA
import com.comcast.cdn.traffic_control.traffic_router.core.dns.ZoneSignerImplTest.IsRRsetTypeNSEC
import java.util.concurrent.ConcurrentMap
import com.comcast.cdn.traffic_control.traffic_router.core.dns.RRSIGCacheKey
import com.comcast.cdn.traffic_control.traffic_router.core.dns.RRsetKey
import java.util.concurrent.ConcurrentHashMap
import com.comcast.cdn.traffic_control.traffic_router.core.dns.SignatureManager
import com.comcast.cdn.traffic_control.traffic_router.core.util.TrafficOpsUtils
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker
import org.xbill.DNS.SetResponse
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import org.xbill.DNS.NSECRecord
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.loc.GeolocationDatabaseUpdater
import com.comcast.cdn.traffic_control.traffic_router.core.loc.MaxmindGeolocationService
import com.comcast.cdn.traffic_control.traffic_router.core.loc.GeoTest
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AnonymousIp
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AnonymousIpDatabaseService
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AnonymousIpWhitelist
import java.io.IOException
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNode
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNodeTest
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNodeException
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeo
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoResult
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoResult.RegionalGeoResultType
import com.comcast.cdn.traffic_control.traffic_router.geolocation.GeolocationException
import java.net.MalformedURLException
import com.comcast.cdn.traffic_control.traffic_router.core.router.HTTPRouteResult
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache.DeliveryServiceReference
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation.LocalizationMethod
import com.comcast.cdn.traffic_control.traffic_router.core.loc.MaxmindGeoIP2Test
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoRule.PostalsType
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoRule
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNode.SuperNode
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoCoordinateRange
import com.comcast.cdn.traffic_control.traffic_router.core.loc.Federation
import com.comcast.cdn.traffic_control.traffic_router.core.util.CidrAddress
import com.comcast.cdn.traffic_control.traffic_router.core.util.ComparableTreeSet
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationMapping
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationRegistry
import com.comcast.cdn.traffic_control.traffic_router.core.edge.InetRecord
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationsBuilder
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtilsException
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AbstractServiceUpdater
import java.nio.file.Path
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AbstractServiceUpdaterTest.Updater
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationMappingBuilder
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AnonymousIpDatabaseServiceTest
import com.maxmind.geoip2.model.AnonymousIpResponse
import com.maxmind.geoip2.exception.GeoIp2Exception
import java.util.TreeSet
import com.comcast.cdn.traffic_control.traffic_router.core.http.HTTPAccessEventBuilder
import com.comcast.cdn.traffic_control.traffic_router.core.http.HTTPAccessRecord
import java.lang.StringBuffer
import com.comcast.cdn.traffic_control.traffic_router.core.util.Fetcher
import java.io.InputStreamReader
import org.powermock.core.classloader.annotations.PowerMockIgnore
import java.io.BufferedReader
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationsWatcher
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringWatcher
import java.lang.InterruptedException
import com.comcast.cdn.traffic_control.traffic_router.core.util.AbstractResourceWatcherTest
import com.comcast.cdn.traffic_control.traffic_router.core.util.ComparableStringByLength
import com.comcast.cdn.traffic_control.traffic_router.core.config.ConfigHandler
import java.lang.Void
import com.comcast.cdn.traffic_control.traffic_router.shared.CertificateData
import com.comcast.cdn.traffic_control.traffic_router.core.config.CertificateChecker
import com.comcast.cdn.traffic_control.traffic_router.core.hash.ConsistentHasher
import com.comcast.cdn.traffic_control.traffic_router.core.ds.Dispersion
import com.comcast.cdn.traffic_control.traffic_router.core.router.DNSRouteResult
import com.comcast.cdn.traffic_control.traffic_router.core.request.DNSRequest
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkUpdater
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatelessTrafficRouterTest
import com.comcast.cdn.traffic_control.traffic_router.core.router.LocationComparator
import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.comcast.cdn.traffic_control.traffic_router.secure.Pkcs1
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import com.comcast.cdn.traffic_control.traffic_router.core.secure.CertificatesClient
import com.comcast.cdn.traffic_control.traffic_router.core.hash.NumberSearcher
import com.comcast.cdn.traffic_control.traffic_router.core.hash.DefaultHashable
import com.comcast.cdn.traffic_control.traffic_router.core.hash.MD5HashFunction
import com.comcast.cdn.traffic_control.traffic_router.core.hash.Hashable
import com.comcast.cdn.traffic_control.traffic_router.core.util.ExternalTest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.catalina.LifecycleException
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import java.security.KeyStore
import com.comcast.cdn.traffic_control.traffic_router.core.external.RouterTest.ClientSslSocketFactory
import com.comcast.cdn.traffic_control.traffic_router.core.external.RouterTest.TestHostnameVerifier
import org.xbill.DNS.SimpleResolver
import javax.net.ssl.SSLHandshakeException
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpHead
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import javax.net.ssl.SNIHostName
import javax.net.ssl.SNIServerName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSession
import org.hamcrest.number.IsCloseTo
import com.comcast.cdn.traffic_control.traffic_router.core.http.RouterFilter
import java.net.InetSocketAddress
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import com.comcast.cdn.traffic_control.traffic_router.core.external.SteeringTest
import com.comcast.cdn.traffic_control.traffic_router.core.external.ConsistentHashTest
import com.comcast.cdn.traffic_control.traffic_router.core.external.DeliveryServicesTest
import com.comcast.cdn.traffic_control.traffic_router.core.external.LocationsTest
import com.comcast.cdn.traffic_control.traffic_router.core.external.RouterTest
import com.comcast.cdn.traffic_control.traffic_router.core.external.StatsTest
import com.comcast.cdn.traffic_control.traffic_router.core.external.ZonesTest
import com.comcast.cdn.traffic_control.traffic_router.core.CatalinaTrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.core.external.HttpDataServer
import com.comcast.cdn.traffic_control.traffic_router.core.external.ExternalTestSuite
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.PatternLayout
import org.junit.AfterClass
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import org.hamcrest.number.OrderingComparison
import javax.management.MBeanServer
import javax.management.ObjectName
import com.comcast.cdn.traffic_control.traffic_router.shared.DeliveryServiceCertificatesMBean
import com.comcast.cdn.traffic_control.traffic_router.shared.DeliveryServiceCertificates
import org.springframework.context.support.FileSystemXmlApplicationContext
import kotlin.jvm.JvmStatic
import org.apache.catalina.startup.Catalina
import org.apache.catalina.core.StandardService
import java.util.stream.Collectors
import org.apache.catalina.core.StandardHost
import org.apache.catalina.core.StandardContext
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mockito
import java.net.URL
import java.util.ArrayList
import java.util.Date
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
        Mockito.`when`(request.getRequestURL()).thenReturn(StringBuffer("http://example.com/index.html?foo=bar"))
        Mockito.`when`(request.getMethod()).thenReturn("GET")
        Mockito.`when`(request.getProtocol()).thenReturn("HTTP/1.1")
        Mockito.`when`(request.getRemoteAddr()).thenReturn("192.168.7.6")
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