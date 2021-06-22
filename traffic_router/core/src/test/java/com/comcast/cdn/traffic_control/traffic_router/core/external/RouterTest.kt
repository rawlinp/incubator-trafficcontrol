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
import org.apache.http.ssl.SSLContextBuilder
import org.hamcrest.Matchers
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.categories.Category
import org.xbill.DNS.Lookup
import org.xbill.DNS.Name
import org.xbill.DNS.Type
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.Arrays
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

@Category(ExternalTest::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RouterTest {
    private var httpClient: CloseableHttpClient? = null
    private val cdnDomain = ".thecdn.example.com"
    private var deliveryServiceId: String? = null
    private var deliveryServiceDomain: String? = null
    private val validLocations: MutableList<String> = ArrayList()
    private val httpsOnlyId = "https-only-test"
    private val httpsOnlyDomain = httpsOnlyId + cdnDomain
    private val httpsOnlyLocations: MutableList<String> = ArrayList()
    private val httpsNoCertsId = "https-nocert"
    private val httpsNoCertsDomain = httpsNoCertsId + cdnDomain
    private val httpsNoCertsLocations: MutableList<String> = ArrayList()
    private val httpAndHttpsId = "http-and-https-test"
    private val httpAndHttpsDomain = httpAndHttpsId + cdnDomain
    private val httpAndHttpsLocations: MutableList<String> = ArrayList()
    private val httpToHttpsId = "http-to-https-test"
    private val httpToHttpsDomain = httpToHttpsId + cdnDomain
    private val httpToHttpsLocations: MutableList<String> = ArrayList()
    private val httpOnlyId = "http-only-test"
    private val httpOnlyDomain = httpOnlyId + cdnDomain
    private val httpOnlyLocations: MutableList<String> = ArrayList()
    private val routerHttpPort = System.getProperty("routerHttpPort", "8888")
    private val routerSecurePort = System.getProperty("routerSecurePort", "8443")
    private val testHttpPort = System.getProperty("testHttpServerPort", "8889")
    private var trustStore: KeyStore? = null
    private val routerDnsPort = Integer.valueOf(System.getProperty("dns.udp.port", "1053"))
    @Before
    @Throws(Exception::class)
    fun before() {
        val objectMapper = ObjectMapper(JsonFactory())
        var resourcePath = "api/2.0/steering"
        var inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        val steeringDeliveryServices: MutableSet<String> = HashSet()
        val steeringData = objectMapper.readTree(inputStream)["response"]
        val elements = steeringData.elements()
        while (elements.hasNext()) {
            val ds = elements.next()
            val dsId = ds["deliveryService"].asText()
            steeringDeliveryServices.add(dsId)
        }
        resourcePath = "publish/CrConfig.json"
        inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            Assert.fail("Could not find file '$resourcePath' needed for test from the current classpath as a resource!")
        }
        val jsonNode = objectMapper.readTree(inputStream)
        deliveryServiceId = null
        val deliveryServices = jsonNode["deliveryServices"].fieldNames()
        while (deliveryServices.hasNext()) {
            val dsId = deliveryServices.next()
            if (steeringDeliveryServices.contains(dsId)) {
                continue
            }
            val deliveryServiceNode = jsonNode["deliveryServices"][dsId]
            val matchsets: Iterator<JsonNode> = deliveryServiceNode["matchsets"].iterator()
            while (matchsets.hasNext() && deliveryServiceId == null) {
                if ("HTTP" == matchsets.next()["protocol"].asText()) {
                    val sslEnabled = JsonUtils.optBoolean(deliveryServiceNode, "sslEnabled")
                    if (!sslEnabled) {
                        deliveryServiceId = dsId
                        deliveryServiceDomain = deliveryServiceNode["domains"][0].asText()
                    }
                }
            }
        }
        Assert.assertThat(deliveryServiceId, IsNot.not(Matchers.nullValue()))
        Assert.assertThat(deliveryServiceDomain, IsNot.not(Matchers.nullValue()))
        Assert.assertThat(httpsOnlyId, IsNot.not(Matchers.nullValue()))
        Assert.assertThat(httpsOnlyDomain, IsNot.not(Matchers.nullValue()))
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
                validLocations.add("http://$cacheId.$deliveryServiceDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
                validLocations.add("http://$cacheId.$deliveryServiceDomain$portText/stuff?fakeClientIpAddress=12.34.56.78&format=json")
            }
            if (cacheNode["deliveryServices"].has(httpsOnlyId)) {
                val port = if (cacheNode.has("httpsPort")) cacheNode["httpsPort"].asInt(443) else 443
                val portText = if (port == 443) "" else ":$port"
                httpsOnlyLocations.add("https://$cacheId.$httpsOnlyDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
            }
            if (cacheNode["deliveryServices"].has(httpsNoCertsId)) {
                val port = if (cacheNode.has("httpsPort")) cacheNode["httpsPort"].asInt(443) else 443
                val portText = if (port == 443) "" else ":$port"
                httpsNoCertsLocations.add("https://$cacheId.$httpsNoCertsDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
            }
            if (cacheNode["deliveryServices"].has(httpAndHttpsId)) {
                var port = if (cacheNode.has("httpsPort")) cacheNode["httpsPort"].asInt(443) else 443
                var portText = if (port == 443) "" else ":$port"
                httpAndHttpsLocations.add("https://$cacheId.$httpAndHttpsDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
                port = if (cacheNode.has("port")) cacheNode["port"].asInt(80) else 80
                portText = if (port == 80) "" else ":$port"
                httpAndHttpsLocations.add("http://$cacheId.$httpAndHttpsDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
            }
            if (cacheNode["deliveryServices"].has(httpToHttpsId)) {
                val port = if (cacheNode.has("httpsPort")) cacheNode["httpsPort"].asInt(443) else 443
                val portText = if (port == 443) "" else ":$port"
                httpToHttpsLocations.add("https://$cacheId.$httpToHttpsDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
            }
            if (cacheNode["deliveryServices"].has(httpOnlyId)) {
                val port = if (cacheNode.has("port")) cacheNode["port"].asInt(80) else 80
                val portText = if (port == 80) "" else ":$port"
                httpOnlyLocations.add("http://$cacheId.$httpOnlyDomain$portText/stuff?fakeClientIpAddress=12.34.56.78")
            }
        }
        Assert.assertThat(validLocations.isEmpty(), IsEqual.equalTo(false))
        Assert.assertThat(httpsOnlyLocations.isEmpty(), IsEqual.equalTo(false))
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val keystoreStream = javaClass.classLoader.getResourceAsStream("keystore.jks")
        trustStore.load(keystoreStream, "changeit".toCharArray())
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).init(trustStore)
        httpClient = HttpClientBuilder.create()
            .setSSLSocketFactory(ClientSslSocketFactory("tr.https-only-test.thecdn.example.com"))
            .setSSLHostnameVerifier(TestHostnameVerifier())
            .disableRedirectHandling()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun after() {
        if (httpClient != null) {
            httpClient!!.close()
        }
    }

    @Test
    @Throws(TextParseException::class, UnknownHostException::class)
    fun itAUsesEdgeTrafficRoutersForHttpRouting() {
        val edgeIpAddresses: MutableSet<String> = HashSet()
        // this will actually be the "miss" scenario since the resolver is localhost, which will be a CZF miss
        // in the miss case, we serve one TR from each location, and these are what we'd serve with our test CrConfig.json
        edgeIpAddresses.add("12.34.21.2")
        edgeIpAddresses.add("12.34.21.3")
        edgeIpAddresses.add("12.34.21.7")
        edgeIpAddresses.add("12.34.21.8")
        edgeIpAddresses.add("2001:dead:beef:124:1:0:0:2")
        edgeIpAddresses.add("2001:dead:beef:124:1:0:0:3")
        edgeIpAddresses.add("2001:dead:beef:124:1:0:0:7")
        edgeIpAddresses.add("2001:dead:beef:124:1:0:0:8")
        val resolver = SimpleResolver("localhost")
        resolver.setPort(routerDnsPort)
        for (type in Arrays.asList(Type.A, Type.AAAA)) {
            val lookup = Lookup(Name("tr.http-only-test.thecdn.example.com."), type)
            lookup.setResolver(resolver)
            lookup.run()
            Assert.assertThat(lookup.result, IsEqual.equalTo(Lookup.SUCCESSFUL))
            Assert.assertThat(lookup.answers.size, IsEqual.equalTo(4))
            for (record in lookup.answers) {
                Assert.assertThat(record.rdataToString(), Matchers.isIn(edgeIpAddresses))
            }
        }
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun itRedirectsValidHttpRequests() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$deliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val header = response.getFirstHeader("Location")
            Assert.assertThat(header.value, Matchers.isIn(validLocations))
            Assert.assertThat(header.value, Matchers.startsWith("http://"))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itDoesRoutingThroughPathsStartingWithCrs() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/crs/stats?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "foo.$deliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun itConsistentlyRedirectsValidRequests() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$deliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val location = response.getFirstHeader("Location").value
            response.close()
            for (i in 0..99) {
                response = httpClient!!.execute(httpGet)
                Assert.assertThat(response.getFirstHeader("Location").value, IsEqual.equalTo(location))
                response.close()
            }
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun itRejectsInvalidRequests() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "foo.invalid-delivery-service-id.bar")
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
    fun itRedirectsHttpsRequests() {
        val httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpsOnlyId.thecdn.example.com")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val header = response.getFirstHeader("Location")
            Assert.assertThat(header.value, Matchers.isIn(httpsOnlyLocations))
            Assert.assertThat(header.value, Matchers.startsWith("https://"))
            Assert.assertThat(header.value, Matchers.containsString("$httpsOnlyId.thecdn.example.com/stuff"))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsHttpRequestsForHttpsOnlyDeliveryService() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpsOnlyId.bar")
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
    fun itRedirectsFromHttpToHttps() {
        var httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpToHttpsId.bar")
        httpClient!!.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val header = response.getFirstHeader("Location")
            Assert.assertThat(header.value, Matchers.isIn(httpToHttpsLocations))
            Assert.assertThat(header.value, Matchers.startsWith("https://"))
            Assert.assertThat(header.value, Matchers.containsString("$httpToHttpsId.thecdn.example.com"))
            Assert.assertThat(header.value, Matchers.containsString("/stuff"))
        }
        httpClient = HttpClientBuilder.create()
            .setSSLSocketFactory(ClientSslSocketFactory("tr.http-and-https-test.thecdn.example.com"))
            .setSSLHostnameVerifier(TestHostnameVerifier())
            .disableRedirectHandling()
            .build()
        httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpToHttpsId.bar")
        httpClient.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val header = response.getFirstHeader("Location")
            Assert.assertThat(header.value, Matchers.isIn(httpToHttpsLocations))
            Assert.assertThat(header.value, Matchers.startsWith("https://"))
            Assert.assertThat(header.value, Matchers.containsString("$httpToHttpsId.thecdn.example.com"))
            Assert.assertThat(header.value, Matchers.containsString("/stuff"))
        }
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsHttpsRequestsForHttpDeliveryService() {
        val httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$deliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(
                "Response 503 expected got" + response.statusLine.statusCode,
                response.statusLine.statusCode,
                IsEqual.equalTo(503)
            )
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun itPreservesProtocolForHttpAndHttps() {
        var httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpAndHttpsId.bar")
        httpClient!!.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val header = response.getFirstHeader("Location")
            Assert.assertThat(header.value, Matchers.isIn(httpAndHttpsLocations))
            Assert.assertThat(header.value, Matchers.startsWith("http://"))
            Assert.assertThat(header.value, Matchers.containsString("$httpAndHttpsId.thecdn.example.com"))
            Assert.assertThat(header.value, Matchers.containsString("/stuff"))
        }
        httpClient = HttpClientBuilder.create()
            .setSSLSocketFactory(ClientSslSocketFactory("tr.http-and-https-test.thecdn.example.com"))
            .setSSLHostnameVerifier(TestHostnameVerifier())
            .disableRedirectHandling()
            .build()
        httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpAndHttpsId.bar")
        httpClient.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val header = response.getFirstHeader("Location")
            Assert.assertThat(header.value, Matchers.isIn(httpAndHttpsLocations))
            Assert.assertThat(header.value, Matchers.startsWith("https://"))
            Assert.assertThat(header.value, Matchers.containsString("$httpAndHttpsId.thecdn.example.com"))
            Assert.assertThat(header.value, Matchers.containsString("/stuff"))
        }
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsCrConfigWithMissingCert() {
        var httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpOnlyId.bar")
        httpClient!!.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            Assert.assertThat(
                response.getFirstHeader("Location").value, Matchers.isOneOf(
                    "http://edge-cache-000.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                    "http://edge-cache-001.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                    "http://edge-cache-002.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78"
                )
            )
        }
        httpClient = HttpClientBuilder.create()
            .setSSLSocketFactory(ClientSslSocketFactory(httpsNoCertsDomain))
            .setSSLHostnameVerifier(TestHostnameVerifier())
            .disableRedirectHandling()
            .build()
        httpGet = HttpGet("https://localhost:$routerSecurePort/x?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpsNoCertsId.bar")
        try {
            httpClient.execute(httpGet).use { response ->
                val code = response.statusLine.statusCode
                Assert.assertThat(
                    "Expected a server error code (503) But got: $code",
                    code, Matchers.greaterThan(500)
                )
            }
        } catch (she: SSLHandshakeException) {
            // Expected result of getting the self-signed _default_ certificate
        }

        // Pretend someone did a cr-config snapshot that would have updated the location to be different
        var httpPost = HttpPost("http://localhost:$testHttpPort/crconfig-2")
        httpClient.execute(httpPost).close()

        // Default interval for polling cr config is 10 seconds
        Thread.sleep((15 * 1000).toLong())
        httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpOnlyId.bar")
        httpClient.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val location = response.getFirstHeader("Location").value
            Assert.assertThat(
                location, IsNot.not(
                    Matchers.isOneOf(
                        "http://edge-cache-010.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                        "http://edge-cache-011.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                        "http://edge-cache-012.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78"
                    )
                )
            )
        }


        // verify that if we get a new cr-config that turns off https for the problematic delivery service
        // that it's able to get through while TR is still concurrently trying to get certs
        var testHttpPort = System.getProperty("testHttpServerPort", "8889")
        httpPost = HttpPost("http://localhost:$testHttpPort/crconfig-3")
        httpClient.execute(httpPost).close()

        // Default interval for polling cr config is 10 seconds
        Thread.sleep((30 * 1000).toLong())
        httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpOnlyId.bar")
        httpClient.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val location = response.getFirstHeader("Location").value
            Assert.assertThat(
                location, Matchers.isOneOf(
                    "http://edge-cache-900.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                    "http://edge-cache-901.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                    "http://edge-cache-902.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78"
                )
            )
        }

        // assert that request gets rejected because SSL is turned off
        httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpsNoCertsId.bar")
        try {
            httpClient.execute(httpGet).use { response ->
                val code = response.statusLine.statusCode
                Assert.assertThat(
                    "Expected an server error code! But got: $code",
                    code, Matchers.greaterThan(500)
                )
            }
        } catch (she: SSLHandshakeException) {
            // expected result of getting the self-signed _default_ certificate
        }

        // Go back to the cr-config that makes the delivery service https again
        // Pretend someone did a cr-config snapshot that would have updated the location to be different
        httpPost = HttpPost("http://localhost:$testHttpPort/crconfig-4")
        httpClient.execute(httpPost).close()

        // Default interval for polling cr config is 10 seconds
        Thread.sleep((15 * 1000).toLong())

        // Update certificates so new ds is valid
        testHttpPort = System.getProperty("testHttpServerPort", "8889")
        httpPost = HttpPost("http://localhost:$testHttpPort/certificates")
        httpClient.execute(httpPost).close()
        httpClient = HttpClientBuilder.create()
            .setSSLSocketFactory(ClientSslSocketFactory("https-additional"))
            .setSSLHostnameVerifier(TestHostnameVerifier())
            .disableRedirectHandling()
            .build()
        // Our initial test cr config data sets cert poller to 10 seconds
        Thread.sleep(25000L)
        httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr." + "https-additional" + ".bar")
        try {
            httpClient.execute(httpGet).use { response ->
                val code = response.statusLine.statusCode
                Assert.assertThat(
                    "Expected an server error code! But got: $code",
                    code, IsEqual.equalTo(302)
                )
            }
        } catch (e: SSLHandshakeException) {
            Assert.fail(e.message)
        }
        httpGet = HttpGet("https://localhost:$routerSecurePort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpsNoCertsId.bar")
        httpClient.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val location = response.getFirstHeader("Location").value
            Assert.assertThat(
                location, Matchers.isOneOf(
                    "https://edge-cache-090.https-nocert.thecdn.example.com/stuff?fakeClientIpAddress=12.34.56.78",
                    "https://edge-cache-091.https-nocert.thecdn.example.com/stuff?fakeClientIpAddress=12.34.56.78",
                    "https://edge-cache-092.https-nocert.thecdn.example.com/stuff?fakeClientIpAddress=12.34.56.78"
                )
            )
        }
        httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78")
        httpGet.addHeader("Host", "tr.$httpOnlyId.bar")
        println(httpGet.toString())
        println(Arrays.toString(httpGet.allHeaders))
        httpClient.execute(httpGet).use { response ->
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(302))
            val location = response.getFirstHeader("Location").value
            Assert.assertThat(
                location, Matchers.isOneOf(
                    "http://edge-cache-010.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                    "http://edge-cache-011.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78",
                    "http://edge-cache-012.http-only-test.thecdn.example.com:8090/stuff?fakeClientIpAddress=12.34.56.78"
                )
            )
        }
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun itDoesUseLocationFormatResponse() {
        val httpGet = HttpGet("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78&format=json")
        httpGet.addHeader("Host", "tr.$deliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpGet)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            val entity = response.entity
            val objectMapper = ObjectMapper(JsonFactory())
            Assert.assertThat(entity.content, IsNot.not(Matchers.nullValue()))
            val json = objectMapper.readTree(entity.content)
            Assert.assertThat(json.has("location"), IsEqual.equalTo(true))
            Assert.assertThat(json["location"].asText(), Matchers.isIn(validLocations))
            Assert.assertThat(json["location"].asText(), Matchers.startsWith("http://"))
        } finally {
            response?.close()
        }
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun itDoesNotUseLocationFormatResponseForHead() {
        val httpHead = HttpHead("http://localhost:$routerHttpPort/stuff?fakeClientIpAddress=12.34.56.78&format=json")
        httpHead.addHeader("Host", "tr.$deliveryServiceId.bar")
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient!!.execute(httpHead)
            Assert.assertThat(response.statusLine.statusCode, IsEqual.equalTo(200))
            Assert.assertThat("Failed getting null body for HEAD request", response.entity, Matchers.nullValue())
        } finally {
            response?.close()
        }
    }

    // This is a workaround to get HttpClient to do the equivalent of
    // curl -v --resolve 'tr.https-only-test.thecdn.cdnlab.example.com:8443:127.0.0.1' https://tr.https-only-test.thecdn.example.com:8443/foo.json
    internal inner class ClientSslSocketFactory(private val host: String) : SSLConnectionSocketFactory(
        SSLContextBuilder.create().loadTrustMaterial(trustStore, TrustSelfSignedStrategy()).build(),
        TestHostnameVerifier()
    ) {
        @Throws(IOException::class)
        override fun prepareSocket(sslSocket: SSLSocket) {
            val serverName = SNIHostName(host)
            val serverNames: MutableList<SNIServerName> = ArrayList(1)
            serverNames.add(serverName)
            val params = sslSocket.sslParameters
            params.serverNames = serverNames
            sslSocket.sslParameters = params
        }
    }

    // This is a workaround for the same reason as above
    // org.apache.http.conn.ssl.SSLConnectionSocketFactory.verifyHostname(<socket>, 'localhost') normally fails
    internal inner class TestHostnameVerifier : HostnameVerifier {
        override fun verify(s: String, sslSession: SSLSession): Boolean {
            Assert.assertThat(
                "s = " + s + ", getPeerHost() = " + sslSession.peerHost,
                sslSession.peerHost,
                IsEqual.equalTo(s)
            )
            return sslSession.peerHost == s
        }
    }
}