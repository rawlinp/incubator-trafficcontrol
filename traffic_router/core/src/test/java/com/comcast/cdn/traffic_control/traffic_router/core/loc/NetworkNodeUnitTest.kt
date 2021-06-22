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
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test

class NetworkNodeUnitTest {
    @Test
    @Throws(Exception::class)
    fun itSupportsARootNode() {
        val root = NetworkNode("0.0.0.0/0")
        val network = NetworkNode("192.168.1.0/24")
        MatcherAssert.assertThat(root.add(network), Matchers.equalTo(true))
        MatcherAssert.assertThat(root.children.entries.iterator().next().key, Matchers.equalTo(network))
    }

    @Test
    @Throws(Exception::class)
    fun itDoesNotAddANodeOutsideOfNetwork() {
        val network = NetworkNode("192.168.0.0/16")
        val subnetwork = NetworkNode("10.10.0.0/16")
        MatcherAssert.assertThat(network.add(subnetwork), Matchers.equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun itFindsIpBelongingToNetwork() {
        val network = NetworkNode("192.168.1.0/24")
        MatcherAssert.assertThat(network.getNetwork("192.168.1.1"), Matchers.equalTo(network))
        MatcherAssert.assertThat(network.getNetwork("192.168.2.1"), Matchers.not(Matchers.equalTo(network)))
    }

    @Test
    @Throws(Exception::class)
    fun itDoesNotAddDuplicates() {
        val supernet = NetworkNode("192.168.0.0/16")
        val network1 = NetworkNode("192.168.1.0/24")
        val duplicate = NetworkNode("192.168.1.0/24")
        MatcherAssert.assertThat(supernet.add(network1), Matchers.equalTo(true))
        MatcherAssert.assertThat(supernet.children.size, Matchers.equalTo(1))
        MatcherAssert.assertThat(supernet.add(duplicate), Matchers.equalTo(false))
        MatcherAssert.assertThat(supernet.children.size, Matchers.equalTo(1))
    }

    @Test
    @Throws(Exception::class)
    fun itPutsNetworksIntoOrderedHierarchy() {
        val root = NetworkNode("0.0.0.0/0")
        val subnet1 = NetworkNode("192.168.6.0/24")
        val subnet2 = NetworkNode("192.168.55.0/24")
        val net = NetworkNode("192.168.0.0/16")
        root.add(net)
        MatcherAssert.assertThat(root.children.entries.iterator().next().key, Matchers.equalTo(net))
        root.add(subnet2)
        root.add(subnet1)
        val iterator: Iterator<Map.Entry<NetworkNode, NetworkNode>> = net.children.entries.iterator()
        MatcherAssert.assertThat(iterator.next().key, Matchers.equalTo(subnet1))
        MatcherAssert.assertThat(iterator.next().key, Matchers.equalTo(subnet2))
    }

    @Test
    @Throws(Exception::class)
    fun itSupportsDeepCaches() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"deepCoverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"1234:5679::/64\"]," +
                "\"caches\": [\"host1\",\"host2\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        val networkNode = NetworkNode.generateTree(json, false, true)
        val foundNetworkNode = networkNode.getNetwork("192.168.55.100")
        val expected: MutableSet<String> = HashSet()
        expected.add("host1")
        expected.add("host2")
        MatcherAssert.assertThat(foundNetworkNode.deepCacheNames, Matchers.equalTo(expected))
    }

    @Test
    @Throws(Exception::class)
    fun itDoesIpV6() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"1234:5679::/64\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        val networkNode = NetworkNode.generateTree(json, false)
        val foundNetworkNode = networkNode.getNetwork("1234:5678::1")
        MatcherAssert.assertThat(foundNetworkNode.loc, Matchers.equalTo("us-co-denver"))
    }

    @Test
    @Throws(Exception::class)
    fun itPutsAllSubnetsUnderSuperNet() {
        val root = NetworkNode("0.0.0.0/0")
        val subnet1 = NetworkNode("192.168.6.0/24")
        root.add(subnet1)
        val subnet2 = NetworkNode("192.168.55.0/24")
        root.add(subnet2)
        val net = NetworkNode("192.168.0.0/16")
        root.add(net)
        MatcherAssert.assertThat(root.children.isEmpty(), Matchers.equalTo(false))
        val generation1Node = root.children.values.iterator().next()
        MatcherAssert.assertThat(generation1Node.toString(), Matchers.equalTo("[192.168.0.0/16] - location:null"))
        val iterator: Iterator<Map.Entry<NetworkNode, NetworkNode>> = generation1Node.children.entries.iterator()
        val generation2FirstNode = iterator.next().key
        val generation2SecondNode = iterator.next().key
        MatcherAssert.assertThat(generation2FirstNode.toString(), Matchers.equalTo("[192.168.6.0/24] - location:null"))
        MatcherAssert.assertThat(
            generation2SecondNode.toString(),
            Matchers.equalTo("[192.168.55.0/24] - location:null")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itMatchesIpsInOverlappingSubnets() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"0:0:0:0:0:ffff:a4f:3700/24\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        val networkNode = NetworkNode.generateTree(json, false)
        val foundNetworkNode = networkNode.getNetwork("192.168.55.2")
        MatcherAssert.assertThat(foundNetworkNode.loc, Matchers.equalTo("us-co-denver"))
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsInvalidIpV4Network() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/40\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"1234:5679::/64\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        MatcherAssert.assertThat(NetworkNode.generateTree(json, false), Matchers.equalTo(null))
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsInvalidIpV6Network() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"zyx:5679::/64\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        MatcherAssert.assertThat(NetworkNode.generateTree(json, false), Matchers.equalTo(null))
    }
}