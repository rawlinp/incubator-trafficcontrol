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
package com.comcast.cdn.traffic_control.traffic_router.core.hashing

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
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.support.FileSystemXmlApplicationContext
import kotlin.jvm.JvmStatic
import org.apache.catalina.startup.Catalina
import org.apache.catalina.core.StandardService
import java.util.stream.Collectors
import org.apache.catalina.core.StandardHost
import org.apache.catalina.core.StandardContext
import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.Random

class ConsistentHasherTest {
    @Mock
    var md5HashFunction = MD5HashFunction()

    @Mock
    var numberSearcher = NumberSearcher()

    @InjectMocks
    var hashable1 = DefaultHashable()

    @InjectMocks
    var hashable2 = DefaultHashable()

    @InjectMocks
    var hashable3 = DefaultHashable()
    var hashables: MutableList<DefaultHashable> = ArrayList()

    @InjectMocks
    var consistentHasher: ConsistentHasher? = null
    var trafficRouter: TrafficRouter? = null
    @Before
    fun before() {
        hashable1.generateHashes("hashId1", 100)
        hashable2.generateHashes("hashId2", 100)
        hashable3.generateHashes("hashId3", 100)
        hashables.add(hashable1)
        hashables.add(hashable2)
        hashables.add(hashable3)
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        Mockito.`when`(trafficRouter!!.buildPatternBasedHashString(Matchers.anyString(), Matchers.anyString()))
            .thenCallRealMethod()
        Mockito.`when`(
            trafficRouter!!.buildPatternBasedHashString(
                Matchers.any(
                    DeliveryService::class.java
                ), Matchers.any(HTTPRequest::class.java)
            )
        ).thenCallRealMethod()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun itHashes() {
        val mapper = ObjectMapper()
        val hashable =
            consistentHasher!!.selectHashable(hashables, Dispersion(mapper.createObjectNode()), "some-string")
        Assert.assertThat(
            hashable,
            org.hamcrest.Matchers.anyOf(
                IsEqual.equalTo(hashable1),
                IsEqual.equalTo(hashable2),
                IsEqual.equalTo(hashable3)
            )
        )
        val nextHashable =
            consistentHasher!!.selectHashable(hashables, Dispersion(mapper.createObjectNode()), "some-string")
        Assert.assertThat(nextHashable, IsEqual.equalTo(hashable))
    }

    @Test
    @Throws(Exception::class)
    fun itHashesMoreThanOne() {
        val jsonStr = """
            {"dispersion": {
            "limit": 2,
            "shuffled": "true"
            }}
            """.trimIndent()
        val mapper = ObjectMapper()
        val jo = mapper.readTree(jsonStr)
        val dispersion = Dispersion(jo)
        val results = consistentHasher!!.selectHashables(hashables, dispersion, "some-string")
        Assert.assertThat(results.size, IsEqual.equalTo(2))
        Assert.assertThat(
            results[0],
            org.hamcrest.Matchers.anyOf(
                IsEqual.equalTo(hashable1),
                IsEqual.equalTo(hashable2),
                IsEqual.equalTo(hashable3)
            )
        )
        Assert.assertThat(
            results[1],
            org.hamcrest.Matchers.anyOf(
                IsEqual.equalTo(hashable1),
                IsEqual.equalTo(hashable2),
                IsEqual.equalTo(hashable3)
            )
        )
        val results2 = consistentHasher!!.selectHashables(hashables, dispersion, "some-string")
        assert(results.containsAll(results2))
        val jsonStr2 = """
            {"dispersion": {
            "limit": 2000000000,
            "shuffled": "true"
            }}
            """.trimIndent()
        val jo2 = mapper.readTree(jsonStr2)
        val disp2 = Dispersion(jo2)
        val res3 = consistentHasher!!.selectHashables(hashables, disp2, "some-string")
        assert(res3.containsAll(hashables))
    }

    @Test
    fun itemsMigrateFromSmallerToLargerBucket() {
        val randomPaths: MutableList<String> = ArrayList()
        for (i in 0..9999) {
            randomPaths.add(generateRandomPath())
        }
        val smallerBucket: Hashable<*> = DefaultHashable().generateHashes("Small One", 10000)
        val largerBucket: Hashable<*> = DefaultHashable().generateHashes("Larger bucket", 90000)
        val buckets: MutableList<Hashable<*>> = ArrayList()
        buckets.add(smallerBucket)
        buckets.add(largerBucket)
        val hashedPaths: MutableMap<Hashable<*>, MutableList<String>> = HashMap()
        hashedPaths[smallerBucket] = ArrayList()
        hashedPaths[largerBucket] = ArrayList()
        val mapper = ObjectMapper()
        for (randomPath in randomPaths) {
            val hashable = consistentHasher!!.selectHashable(buckets, Dispersion(mapper.createObjectNode()), randomPath)
            hashedPaths[hashable]!!.add(randomPath)
        }
        val grownBucket: Hashable<*> = DefaultHashable().generateHashes("Small One", 20000)
        val shrunkBucket: Hashable<*> = DefaultHashable().generateHashes("Larger bucket", 80000)
        val changedBuckets: MutableList<Hashable<*>> = ArrayList()
        changedBuckets.add(grownBucket)
        changedBuckets.add(shrunkBucket)
        val rehashedPaths: MutableMap<Hashable<*>, MutableList<String>> = HashMap()
        rehashedPaths[grownBucket] = ArrayList()
        rehashedPaths[shrunkBucket] = ArrayList()
        for (randomPath in randomPaths) {
            val hashable =
                consistentHasher!!.selectHashable(changedBuckets, Dispersion(mapper.createObjectNode()), randomPath)
            rehashedPaths[hashable]!!.add(randomPath)
        }
        Assert.assertThat(
            rehashedPaths[grownBucket]!!.size, org.hamcrest.Matchers.greaterThan(
                hashedPaths[smallerBucket]!!.size
            )
        )
        Assert.assertThat(
            rehashedPaths[shrunkBucket]!!.size, org.hamcrest.Matchers.lessThan(
                hashedPaths[largerBucket]!!.size
            )
        )
        for (path in hashedPaths[smallerBucket]!!) {
            Assert.assertThat(rehashedPaths[grownBucket]!!.contains(path), IsEqual.equalTo(true))
        }
        for (path in rehashedPaths[shrunkBucket]!!) {
            Assert.assertThat(hashedPaths[largerBucket]!!.contains(path), IsEqual.equalTo(true))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testPatternBasedHashing() {
        // use regex to standardize path
        val regex = "/.*?(/.*?/).*?(.m3u8)"
        val expectedResult = "/some_stream_name1234/.m3u8"
        var requestPath = "/path12341234/some_stream_name1234/some_info4321.m3u8"
        var pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult1 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        requestPath = "/pathasdf1234/some_stream_name1234/some_other_info.m3u8"
        pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult2 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        requestPath = "/path4321fdsa/some_stream_name1234/4321some_info.m3u8"
        pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult3 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        requestPath = "/1234pathfdas/some_stream_name1234/some_info.m3u8"
        pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult4 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        Assert.assertThat(
            hashableResult1,
            org.hamcrest.Matchers.allOf(
                IsEqual.equalTo(hashableResult2),
                IsEqual.equalTo(hashableResult3),
                IsEqual.equalTo(hashableResult4)
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itHashesQueryParams() {
        val j =
            ObjectMapper().readTree("{\"routingName\":\"edge\",\"coverageZoneOnly\":false,\"consistentHashQueryParams\":[\"test\", \"quest\"]}")
        val d = DeliveryService("test", j)
        val r1 = HTTPRequest()
        r1.path = "/path1234/some_stream_name1234/some_other_info.m3u8"
        r1.queryString = "test=value"
        val r2 = HTTPRequest()
        r2.path = r1.path
        r2.queryString = "quest=other_value"
        val p1 = trafficRouter!!.buildPatternBasedHashString(d, r1)
        val p2 = trafficRouter!!.buildPatternBasedHashString(d, r2)
        assert(p1 != p2)
    }

    var alphanumericCharacters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZYZ"
    var exampleValidPathCharacters = "$alphanumericCharacters/=;()-."
    var random = Random(1462307930227L)
    fun generateRandomPath(): String {
        val pathLength = 60 + random.nextInt(61)
        val stringBuilder = StringBuilder("/")
        for (i in 0..3) {
            val index = random.nextInt(alphanumericCharacters.length)
            stringBuilder.append(alphanumericCharacters[index])
        }
        stringBuilder.append("/")
        for (i in 0 until pathLength) {
            val index = random.nextInt(exampleValidPathCharacters.length)
            stringBuilder.append(exampleValidPathCharacters[index])
        }
        return stringBuilder.toString()
    }
}