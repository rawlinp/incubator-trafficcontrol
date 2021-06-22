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
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.support.FileSystemXmlApplicationContext
import kotlin.jvm.JvmStatic
import org.apache.catalina.startup.Catalina
import org.apache.catalina.core.StandardService
import java.util.stream.Collectors
import org.apache.catalina.core.StandardHost
import org.apache.catalina.core.StandardContext
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.categories.Category
import java.net.URLEncoder
import java.util.ArrayList

@Category(ExternalTest::class)
class ConsistentHashTest {
    private var closeableHttpClient: CloseableHttpClient? = null
    var deliveryServiceId: String? = null
    var ipAddressInCoverageZone: String? = null
    var steeringDeliveryServiceId: String? = null
    var consistentHashRegex: String? = null
    var steeredDeliveryServices: MutableList<String> = ArrayList()
    @Before
    @Throws(Exception::class)
    fun before() {
        closeableHttpClient = HttpClientBuilder.create().build()
        var resourcePath = "api/2.0/steering"
        var inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        val objectMapper = ObjectMapper(JsonFactory())
        val steeringNode = objectMapper.readTree(inputStream)["response"][0]
        steeringDeliveryServiceId = steeringNode["deliveryService"].asText()
        val iterator: Iterator<JsonNode> = steeringNode["targets"].iterator()
        while (iterator.hasNext()) {
            val target = iterator.next()
            steeredDeliveryServices.add(target["deliveryService"].asText())
        }
        resourcePath = "publish/CrConfig.json"
        inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        var jsonNode = objectMapper.readTree(inputStream)
        deliveryServiceId = null
        val deliveryServices = jsonNode["deliveryServices"].fieldNames()
        while (deliveryServices.hasNext() && deliveryServiceId == null) {
            val dsId = deliveryServices.next()
            val deliveryServiceNode = jsonNode["deliveryServices"][dsId]
            if (deliveryServiceNode.has("steeredDeliveryServices")) {
                continue
            }
            val dispersionNode = deliveryServiceNode["dispersion"]
            if (dispersionNode == null || dispersionNode["limit"].asInt() != 1 && dispersionNode["shuffled"].asText() == "true") {
                continue
            }
            val matchsets: Iterator<JsonNode> = deliveryServiceNode["matchsets"].iterator()
            while (matchsets.hasNext() && deliveryServiceId == null) {
                if ("HTTP" == matchsets.next()["protocol"].asText()) {
                    if (deliveryServiceNode.has("consistentHashRegex")) {
                        deliveryServiceId = dsId
                        consistentHashRegex = deliveryServiceNode["consistentHashRegex"].asText()
                    }
                }
            }
            if (deliveryServiceId == null) {
                println("Skipping $deliveryServiceId no http protocol matchset")
            }
        }
        Assert.assertThat(deliveryServiceId, IsNot.not(CoreMatchers.nullValue()))
        Assert.assertThat(steeringDeliveryServiceId, IsNot.not(CoreMatchers.nullValue()))
        Assert.assertThat(steeredDeliveryServices.isEmpty(), IsEqual.equalTo(false))
        resourcePath = "czf.json"
        inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        jsonNode = objectMapper.readTree(inputStream)
        val network = jsonNode["coverageZones"][jsonNode["coverageZones"].fieldNames().next()]["network"]
        for (i in 0 until network.size()) {
            val cidrString = network[i].asText()
            val cidrAddress = CidrAddress.fromString(cidrString)
            if (cidrAddress.netmaskLength == 24) {
                val hostBytes = cidrAddress.hostBytes
                ipAddressInCoverageZone = String.format("%d.%d.%d.123", hostBytes[0], hostBytes[1], hostBytes[2])
                break
            }
        }
        Assert.assertThat(ipAddressInCoverageZone!!.length, OrderingComparison.greaterThan(0))
    }

    @After
    @Throws(Exception::class)
    fun after() {
        if (closeableHttpClient != null) closeableHttpClient!!.close()
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesConsistentHashingToRequestsForCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to find $ipAddressInCoverageZone in coverage zone using delivery service id $deliveryServiceId",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/another/different/path", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo(cacheId)))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo("")))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesConsistentHashingForRequestsOutsideCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/another/different/path", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo(cacheId)))
            Assert.assertThat(cacheNode["id"].asText(), IsNot.not(IsEqual.equalTo("")))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesConsistentHashingToSteeringDeliveryService() {
        var response: CloseableHttpResponse? = null
        try {
            val requestPath = URLEncoder.encode("/some/path/thing", "UTF-8")
            val httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            val deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), Matchers.isIn(steeredDeliveryServices))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesBypassFiltersWithDeliveryServiceSteering() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/force-to-target-2/more/asdfasdf", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            var deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), IsEqual.equalTo("steering-target-2"))
            requestPath = URLEncoder.encode("/some/path/force-to-target-1/more/asdfasdf", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), IsEqual.equalTo("steering-target-1"))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesRegexToStandardizeRequestPath() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            val encodedConsistentHashRegex = URLEncoder.encode(consistentHashRegex, "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/patternbased/regex?regex=$encodedConsistentHashRegex&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to get 200 response from /consistenthash/patternbased/regex endpoint",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var resp = objectMapper.readTree(EntityUtils.toString(response.entity))
            val resultingPathToConsistentHash = resp["resultingPathToConsistentHash"].asText()
            requestPath = URLEncoder.encode("/other/path/other_thing.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/patternbased/regex?regex=$encodedConsistentHashRegex&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            resp = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(
                JsonUtils.optString(resp, "resultingPathToConsistentHash"),
                IsEqual.equalTo(resultingPathToConsistentHash)
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingToRequestsForCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to find $ipAddressInCoverageZone in coverage zone using delivery service id $deliveryServiceId",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/other/path/other_thing.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone?ip=$ipAddressInCoverageZone&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingForRequestsOutsideCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath = URLEncoder.encode("/other/path/other_thing.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/geolocation?ip=8.8.8.8&deliveryServiceId=$deliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingToSteeringDeliveryService() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            val objectMapper = ObjectMapper(JsonFactory())
            var deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val deliveryServiceId = deliveryServiceNode["id"].asText()
            Assert.assertThat(deliveryServiceId, Matchers.isIn(steeredDeliveryServices))
            response.close()
            requestPath =
                URLEncoder.encode("/other_different_path_12344321/path/other_thing_to_hash_differently.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/deliveryservice?ip=98.76.54.123&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            deliveryServiceNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(deliveryServiceNode["id"].asText(), IsEqual.equalTo(deliveryServiceId))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itAppliesPatternBasedConsistentHashingToSteeringRequestsForCoverageZone() {
        var response: CloseableHttpResponse? = null
        try {
            var requestPath = URLEncoder.encode("/some/path/thing.m3u8", "UTF-8")
            var httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone/steering?ip=$ipAddressInCoverageZone&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            Assert.assertThat(
                "Expected to find $ipAddressInCoverageZone in coverage zone using delivery service id $deliveryServiceId",
                response.statusLine.statusCode,
                IsEqual.equalTo(200)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            var cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            val cacheId = cacheNode["id"].asText()
            Assert.assertThat(cacheId, IsNot.not(IsEqual.equalTo("")))
            response.close()
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
            response.close()
            requestPath =
                URLEncoder.encode("/other_different_path_12344321/path/other_thing_to_hash_differently.m3u8", "UTF-8")
            httpGet =
                HttpGet("http://localhost:3333/crs/consistenthash/cache/coveragezone/steering?ip=$ipAddressInCoverageZone&deliveryServiceId=$steeringDeliveryServiceId&requestPath=$requestPath")
            response = closeableHttpClient!!.execute(httpGet)
            cacheNode = objectMapper.readTree(EntityUtils.toString(response.entity))
            Assert.assertThat(cacheNode["id"].asText(), IsEqual.equalTo(cacheId))
        } finally {
            response?.close()
        }
    }
}