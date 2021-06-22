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
import org.hamcrest.Matchers
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.Arrays
import java.util.Random

@Category(ExternalTest::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SteeringTest {
    var steeringDeliveryServiceId: String? = null
    var targetDomains: MutableMap<String, String> = HashMap()
    var targetWeights: MutableMap<String?, Int> = HashMap()
    var httpClient: CloseableHttpClient? = null
    var validLocations: MutableList<String> = ArrayList()
    var routerHttpPort = System.getProperty("routerHttpPort", "8888")
    var testHttpPort = System.getProperty("testHttpServerPort", "8889")
    @Throws(IOException::class)
    fun getJsonForResourcePath(resourcePath: String): JsonNode {
        val objectMapper = ObjectMapper(JsonFactory())
        val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        return objectMapper.readTree(inputStream)["response"][0]
    }

    @Throws(IOException::class)
    fun setupSteering(
        domains: MutableMap<String, String>,
        weights: MutableMap<String?, Int>,
        resourcePath: String
    ): String {
        domains.clear()
        weights.clear()
        val steeringNode = getJsonForResourcePath(resourcePath)
        val steeredDeliveryServices: Iterator<JsonNode> = steeringNode["targets"].iterator()
        while (steeredDeliveryServices.hasNext()) {
            val steeredDeliveryService = steeredDeliveryServices.next()
            val targetId = steeredDeliveryService["deliveryService"].asText()
            val targetWeight = steeredDeliveryService["weight"].asInt()
            weights[targetId] = targetWeight
            domains[targetId] = ""
        }
        //System.out.println("steeringNode.get = "+ steeringNode.get("deliveryService").asText());
        return steeringNode["deliveryService"].asText()
    }

    @Throws(IOException::class)
    fun setupCrConfig() {
        val resourcePath = "publish/CrConfig.json"
        val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        val jsonNode = ObjectMapper(JsonFactory()).readTree(inputStream)
        val deliveryServices = jsonNode["deliveryServices"].fieldNames()
        while (deliveryServices.hasNext()) {
            val dsId = deliveryServices.next()
            if (targetDomains.containsKey(dsId)) {
                targetDomains[dsId] = jsonNode["deliveryServices"][dsId]["domains"][0].asText()
            }
        }
        Assert.assertThat(steeringDeliveryServiceId, IsNot.not(Matchers.nullValue()))
        Assert.assertThat(targetDomains.isEmpty(), IsEqual.equalTo(false))
        for (deliveryServiceId in targetDomains.keys) {
            val cacheIds = jsonNode["contentServers"].fieldNames()
            while (cacheIds.hasNext()) {
                val cacheId = cacheIds.next()
                val cacheNode = jsonNode["contentServers"][cacheId]
                if (!cacheNode.has("deliveryServices")) {
                    continue
                }
                if (cacheNode["deliveryServices"].has(deliveryServiceId)) {
                    val port = cacheNode["port"].asInt()
                    val portText = if (port == 80) "" else ":$port"
                    validLocations.add("http://" + cacheId + "." + targetDomains[deliveryServiceId] + portText + "/stuff?fakeClientIpAddress=12.34.56.78")
                }
            }
        }
        Assert.assertThat(validLocations.isEmpty(), IsEqual.equalTo(false))
    }

    @Before
    @Throws(Exception::class)
    fun before() {
        steeringDeliveryServiceId = setupSteering(targetDomains, targetWeights, "api/2.0/steering")
        setupCrConfig()
        httpClient = HttpClientBuilder.create().disableRedirectHandling().build()
    }

    @Test
    @Throws(Exception::class)
    fun itUsesSteeredDeliveryServiceIdInRedirect() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "foo.$steeringDeliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(
                "Failed getting 302 for request " + httpGet.getFirstHeader("Host").value,
                response.statusLine.statusCode,
                IsEqual.equalTo(302)
            )
            Assert.assertThat(response.getFirstHeader("Location").value, Matchers.isIn(validLocations))
            //System.out.println("itUsesSteered = "+response.getFirstHeader("Location").getValue());
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesTargetFiltersForSteering() {
        val httpGet =
            HttpGet("http://localhost:$routerHttpPort/qwerytuiop/force-to-target-2/asdfghjkl?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "foo.steering-test-1.thecdn.example.com")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(
                "Failed getting 302 for request " + httpGet.getFirstHeader("Host").value,
                response.statusLine.statusCode,
                IsEqual.equalTo(302)
            )
            Assert.assertThat(
                response.getFirstHeader("Location").value,
                Matchers.endsWith(".steering-target-2.thecdn.example.com:8090/qwerytuiop/force-to-target-2/asdfghjkl?fakeClientIpAddress=12.34.56.78")
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesXtcSteeringOptionForOverride() {
        val httpGet =
            HttpGet("http://localhost:$routerHttpPort/qwerytuiop/force-to-target-2/asdfghjkl?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "foo.steering-test-1.thecdn.example.com")
        httpGet.addHeader("X-TC-Steering-Option", "steering-target-1")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(
                "Failed getting 302 for request " + httpGet.getFirstHeader("Host").value,
                response.statusLine.statusCode,
                IsEqual.equalTo(302)
            )
            Assert.assertThat(
                response.getFirstHeader("Location").value,
                Matchers.endsWith(".steering-target-1.thecdn.example.com:8090/qwerytuiop/force-to-target-2/asdfghjkl?fakeClientIpAddress=12.34.56.78")
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itReturns503ForBadDeliveryServiceInXtcSteeringOption() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "foo.steering-test-1.thecdn.example.com")
        httpGet.addHeader("X-TC-Steering-Option", "ds-02")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(503))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesWeightedDistributionForRequestPath() {
        var count = 0
        for (weight in targetWeights.values) {
            count += weight
        }
        count *= 1000
        if (count > 100000) {
            count = 100000
        }
        val results: MutableMap<String?, Int> = HashMap()
        for (steeredId in targetWeights.keys) {
            results[steeredId] = 0
        }

        //System.out.println("Going to execute " + count + " requests through steering delivery service '" + steeringDeliveryServiceId + "'");
        for (i in 0 until count) {
            val path = generateRandomPath()
            val httpGet = HttpGet("http://localhost:$routerHttpPort$path?fakeClientIpAddress=12.34.56.78")
            httpGet.addHeader("Host", "foo.$steeringDeliveryServiceId.bar")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpGet)
                Assert.assertThat(
                    "Did not get 302 for request '" + httpGet.uri + "'",
                    response.statusLine.statusCode,
                    IsEqual.equalTo(302)
                )
                val location = response.getFirstHeader("Location").value
                for (id in results.keys) {
                    if (location.contains(id!!)) {
                        results[id] = results[id]!! + 1
                    }
                }
            } finally {
                response?.close()
            }
        }
        var totalWeight = 0.0
        for (weight in targetWeights.values) {
            totalWeight += weight.toDouble()
        }
        val expectedHitRates: MutableMap<String?, Double> = HashMap()
        for (id in targetWeights.keys) {
            expectedHitRates[id] = targetWeights[id]!! / totalWeight
        }
        for (id in results.keys) {
            val hits = results[id]!!
            val hitRate = hits.toDouble() / count
            Assert.assertThat(hitRate, IsCloseTo.closeTo(expectedHitRates[id]!!, 0.009))
        }
    }

    @Test
    @Throws(Exception::class)
    fun z_itemsMigrateFromSmallerToLargerBucket() {
        val domains: MutableMap<String, String> = HashMap()
        val weights: MutableMap<String?, Int> = HashMap()
        setupSteering(domains, weights, "api/2.0/steering2")
        val randomPaths: MutableList<String> = ArrayList()
        for (i in 0..9999) {
            randomPaths.add(generateRandomPath())
        }
        var smallerTarget: String? = null
        var largerTarget: String? = null
        for (target in weights.keys) {
            if (smallerTarget == null && largerTarget == null) {
                smallerTarget = target
                largerTarget = target
            }
            if (weights[smallerTarget]!! > weights[target]!!) {
                smallerTarget = target
            }
            if (weights[largerTarget]!! < weights[target]!!) {
                largerTarget = target
            }
        }
        val hashedPaths: MutableMap<String?, MutableList<String>> = HashMap()
        hashedPaths[smallerTarget] = ArrayList()
        hashedPaths[largerTarget] = ArrayList()
        for (path in randomPaths) {
            val httpGet = HttpGet("http://localhost:$routerHttpPort$path?fakeClientIpAddress=12.34.56.78")
            httpGet.addHeader("Host", "foo.$steeringDeliveryServiceId.bar")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpGet)
                Assert.assertThat(
                    "Did not get 302 for request '" + httpGet.uri + "'",
                    response.statusLine.statusCode,
                    IsEqual.equalTo(302)
                )
                val location = response.getFirstHeader("Location").value
                for (targetXmlId in hashedPaths.keys) {
                    if (location.contains(targetXmlId!!)) {
                        hashedPaths[targetXmlId]!!.add(path)
                    }
                }
            } finally {
                response?.close()
            }
        }

        // Change the steering attributes
        val httpPost = HttpPost("http://localhost:$testHttpPort/steering")
        httpClient!!.execute(httpPost).close()

        // a polling interval of 60 seconds is common
        Thread.sleep((90 * 1000).toLong())
        val rehashedPaths: MutableMap<String?, MutableList<String>> = HashMap()
        rehashedPaths[smallerTarget] = ArrayList()
        rehashedPaths[largerTarget] = ArrayList()
        for (path in randomPaths) {
            val httpGet = HttpGet("http://localhost:$routerHttpPort$path?fakeClientIpAddress=12.34.56.78")
            httpGet.addHeader("Host", "foo.$steeringDeliveryServiceId.bar")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpGet)
                Assert.assertThat(
                    "Did not get 302 for request '" + httpGet.uri + "'",
                    response.statusLine.statusCode,
                    IsEqual.equalTo(302)
                )
                val location = response.getFirstHeader("Location").value
                for (targetXmlId in rehashedPaths.keys) {
                    if (location.contains(targetXmlId!!)) {
                        rehashedPaths[targetXmlId]!!.add(path)
                    }
                }
            } finally {
                response?.close()
            }
        }
        Assert.assertThat(
            rehashedPaths[smallerTarget]!!.size, Matchers.greaterThan(
                hashedPaths[smallerTarget]!!.size
            )
        )
        Assert.assertThat(
            rehashedPaths[largerTarget]!!.size, Matchers.lessThan(
                hashedPaths[largerTarget]!!.size
            )
        )
        for (path in hashedPaths[smallerTarget]!!) {
            Assert.assertThat(rehashedPaths[smallerTarget]!!.contains(path), IsEqual.equalTo(true))
            Assert.assertThat(rehashedPaths[largerTarget]!!.contains(path), IsEqual.equalTo(false))
        }
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

    @Test
    @Throws(Exception::class)
    fun itUsesMultiLocationFormatResponse() {
        val paths: MutableList<String> = ArrayList()
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=true")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=TRUE")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=TruE")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=T")
        for (path in paths) {
            val httpGet = HttpGet("http://localhost:$routerHttpPort$path")
            httpGet.addHeader("Host", "tr.client-steering-test-1.thecdn.example.com")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpGet)
                val location1 = ".client-steering-target-2.thecdn.example.com:8090$path"
                val location2 = ".client-steering-target-1.thecdn.example.com:8090$path"
                Assert.assertThat(
                    "Failed getting 302 for request " + httpGet.getFirstHeader("Host").value,
                    response.statusLine.statusCode,
                    IsEqual.equalTo(302)
                )
                Assert.assertThat(response.getFirstHeader("Location").value, Matchers.endsWith(location1))
                val entity = response.entity
                val objectMapper = ObjectMapper(JsonFactory())
                Assert.assertThat(entity.content, IsNot.not(Matchers.nullValue()))
                val json = objectMapper.readTree(entity.content)
                Assert.assertThat(json.has("locations"), IsEqual.equalTo(true))
                Assert.assertThat(json["locations"].size(), IsEqual.equalTo(2))
                Assert.assertThat(
                    json["locations"][0].asText(),
                    IsEqual.equalTo(response.getFirstHeader("Location").value)
                )
                Assert.assertThat(json["locations"][1].asText(), Matchers.endsWith(location2))
            } finally {
                response?.close()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesMultiLocationFormatResponseWithout302() {
        val paths: MutableList<String> = ArrayList()
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=false")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=FALSE")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=FalsE")
        for (path in paths) {
            val httpGet = HttpGet("http://localhost:$routerHttpPort$path")
            httpGet.addHeader("Host", "tr.client-steering-test-1.thecdn.example.com")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpGet)
                val location1 = ".client-steering-target-2.thecdn.example.com:8090$path"
                val location2 = ".client-steering-target-1.thecdn.example.com:8090$path"
                Assert.assertThat(
                    "Failed getting 200 for request " + httpGet.getFirstHeader("Host").value,
                    response.statusLine.statusCode,
                    IsEqual.equalTo(200)
                )
                val entity = response.entity
                val objectMapper = ObjectMapper(JsonFactory())
                Assert.assertThat(entity.content, IsNot.not(Matchers.nullValue()))
                val json = objectMapper.readTree(entity.content)
                Assert.assertThat(json.has("locations"), IsEqual.equalTo(true))
                Assert.assertThat(json["locations"].size(), IsEqual.equalTo(2))
                Assert.assertThat(json["locations"][0].asText(), Matchers.endsWith(location1))
                Assert.assertThat(json["locations"][1].asText(), Matchers.endsWith(location2))
            } finally {
                response?.close()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesNoMultiLocationFormatResponseWithout302WithHead() {
        val paths: MutableList<String> = ArrayList()
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=false")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=FALSE")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=FalsE")
        for (path in paths) {
            val httpHead = HttpHead("http://localhost:$routerHttpPort$path")
            httpHead.addHeader("Host", "tr.client-steering-test-1.thecdn.example.com")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpHead)
                Assert.assertThat(
                    "Failed getting 200 for request " + httpHead.getFirstHeader("Host").value,
                    response.statusLine.statusCode,
                    IsEqual.equalTo(200)
                )
                Assert.assertThat("Failed getting null body for HEAD request", response.entity, Matchers.nullValue())
            } finally {
                response?.close()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesNoMultiLocationFormatResponseWithHead() {
        val paths: MutableList<String> = ArrayList()
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=true")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=TRUE")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=TruE")
        paths.add("/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78&" + RouterFilter.REDIRECT_QUERY_PARAM + "=T")
        for (path in paths) {
            val httpHead = HttpHead("http://localhost:$routerHttpPort$path")
            httpHead.addHeader("Host", "tr.client-steering-test-1.thecdn.example.com")
            var response: CloseableHttpResponse? = null
            try {
                response = httpClient!!.execute(httpHead)
                val location = ".client-steering-target-2.thecdn.example.com:8090$path"
                Assert.assertThat(
                    "Failed getting 302 for request " + httpHead.getFirstHeader("Host").value,
                    response.statusLine.statusCode,
                    IsEqual.equalTo(302)
                )
                Assert.assertThat(response.getFirstHeader("Location").value, Matchers.endsWith(location))
                Assert.assertThat("Failed getting null body for HEAD request", response.entity, Matchers.nullValue())
            } finally {
                response?.close()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun itUsesMultiLocationFormatWithMoreThanTwoEntries() {
        val path = "/qwerytuiop/asdfghjkl?fakeClientIpAddress=12.34.56.78"
        val httpGet = HttpGet("http://localhost:$routerHttpPort$path")
        httpGet.addHeader("Host", "tr.client-steering-test-2.thecdn.example.com")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            val location1 = ".steering-target-2.thecdn.example.com:8090$path"
            val location2 = ".steering-target-1.thecdn.example.com:8090$path"
            val location3 = ".client-steering-target-2.thecdn.example.com:8090$path"
            val location4 = ".client-steering-target-4.thecdn.example.com:8090$path"
            val location5 = ".client-steering-target-3.thecdn.example.com:8090$path"
            val location6 = ".client-steering-target-1.thecdn.example.com:8090$path"
            val location7 = ".steering-target-4.thecdn.example.com:8090$path"
            val location8 = ".steering-target-3.thecdn.example.com:8090$path"
            val entity = response.entity
            Assert.assertThat(
                "Failed getting 302 for request " + httpGet.getFirstHeader("Host").value,
                response.statusLine.statusCode,
                IsEqual.equalTo(302)
            )
            Assert.assertThat(response.getFirstHeader("Location").value, Matchers.endsWith(location1))
            val objectMapper = ObjectMapper(JsonFactory())
            Assert.assertThat(entity.content, IsNot.not(Matchers.nullValue()))
            val json = objectMapper.readTree(entity.content)
            Assert.assertThat(json.has("locations"), IsEqual.equalTo(true))
            Assert.assertThat(json["locations"].size(), IsEqual.equalTo(8))
            Assert.assertThat(json["locations"][0].asText(), IsEqual.equalTo(response.getFirstHeader("Location").value))
            Assert.assertThat(json["locations"][1].asText(), Matchers.endsWith(location2))
            Assert.assertThat(json["locations"][2].asText(), Matchers.endsWith(location3))
            Assert.assertThat(json["locations"][3].asText(), Matchers.endsWith(location4))
            Assert.assertThat(json["locations"][4].asText(), Matchers.endsWith(location5))
            Assert.assertThat(json["locations"][5].asText(), Matchers.endsWith(location6))
            Assert.assertThat(json["locations"][6].asText(), Matchers.endsWith(location7))
            Assert.assertThat(json["locations"][7].asText(), Matchers.endsWith(location8))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itSupportsClientSteeringDiversity() {
        val path = "/foo?fakeClientIpAddress=192.168.42.10" // this IP should get a DEEP_CZ hit (via dczmap.json)
        val httpGet = HttpGet("http://localhost:$routerHttpPort$path")
        httpGet.addHeader("Host", "cdn.client-steering-diversity-test.thecdn.example.com")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            val entity = response.entity
            Assert.assertThat(
                "Failed getting 302 for request " + httpGet.getFirstHeader("Host").value,
                response.statusLine.statusCode,
                IsEqual.equalTo(302)
            )
            val objectMapper = ObjectMapper(JsonFactory())
            Assert.assertThat(entity.content, IsNot.not(Matchers.nullValue()))
            val json = objectMapper.readTree(entity.content)
            Assert.assertThat(json.has("locations"), IsEqual.equalTo(true))
            Assert.assertThat(json["locations"].size(), IsEqual.equalTo(5))
            val actualEdgesList: MutableList<String> = ArrayList()
            val actualTargets: MutableSet<String> = HashSet()
            for (n in json["locations"]) {
                var l = n.asText()
                l = l.replaceFirst("http://".toRegex(), "")
                val parts = l.split("\\.".toRegex()).toTypedArray()
                actualEdgesList.add(parts[0])
                actualTargets.add(parts[1])
            }

            // assert that:
            // - 1st and 2nd targets are edges from the deep cachegroup (because this is a deep hit)
            // - 3rd target is the last unselected edge, which is *not* in the deep cachegroup
            //   (because once all the deep edges have been selected, we select from the regular cachegroup)
            // - 4th and 5th targets are any of the three edges (because all available edges have already been selected)
            val deepEdges: MutableSet<String> = HashSet()
            deepEdges.add("edge-cache-csd-1")
            deepEdges.add("edge-cache-csd-2")
            val allEdges: MutableSet<String> = HashSet(deepEdges)
            allEdges.add("edge-cache-csd-3")
            Assert.assertThat(actualEdgesList[0], Matchers.isIn(deepEdges))
            Assert.assertThat(actualEdgesList[1], Matchers.isIn(deepEdges))
            Assert.assertThat(actualEdgesList[2], IsEqual.equalTo("edge-cache-csd-3"))
            Assert.assertThat(actualEdgesList[3], Matchers.isIn(allEdges))
            Assert.assertThat(actualEdgesList[4], Matchers.isIn(allEdges))

            // assert that all 5 steering targets are included in the response
            val expectedTargetsArray =
                arrayOf("csd-target-1", "csd-target-2", "csd-target-3", "csd-target-4", "csd-target-5")
            val expectedTargets: Set<String> = HashSet(Arrays.asList(*expectedTargetsArray))
            Assert.assertThat<Set<String>>(actualTargets, IsEqual.equalTo(expectedTargets))
        } finally {
            response?.close()
        }
    }
}