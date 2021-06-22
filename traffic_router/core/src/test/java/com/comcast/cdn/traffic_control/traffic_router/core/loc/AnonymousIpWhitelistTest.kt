/*
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
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class AnonymousIpWhitelistTest {
    var ip4whitelist: AnonymousIpWhitelist? = null
    var ip6whitelist: AnonymousIpWhitelist? = null
    @Before
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun setup() {
        val mapper = ObjectMapper()
        ip4whitelist = AnonymousIpWhitelist()
        ip4whitelist!!.init(mapper.readTree("[\"192.168.30.0/24\", \"10.0.2.0/24\", \"10.0.0.0/16\"]"))
        ip6whitelist = AnonymousIpWhitelist()
        ip6whitelist!!.init(mapper.readTree("[\"::1/32\", \"2001::/64\"]"))
    }

    @Test
    fun testAnonymousIpWhitelistConstructor() {
        // final InetAddress address = InetAddresses.forString("192.168.30.1");
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.30.1"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIPsInWhitelist() {
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.30.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.30.254"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.254"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.1.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.254.254"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIPsNotInWhitelist() {
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.31.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.167.30.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.1.1.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.10.1.1"), CoreMatchers.equalTo(false))
    }

    /* IPv6 Testing */
    @Test
    fun testIPv6AddressInWhitelist() {
        MatcherAssert.assertThat(ip6whitelist!!.contains("::1"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIPv6AddressInWhitelistInSubnet() {
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001::"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:0:0:0:1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:0:0:1:1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:a:a:a:a"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:ffff:ffff:ffff:ffff"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIpv6AddressNotInWhitelist() {
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:1:0:0:0:0:0:0"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:1::"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2002:0:0:0:0:0:0:1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:1:ffff:ffff:ffff:ffff"), CoreMatchers.equalTo(false))
    }

    @Test
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun testWhitelistCreationLeafFirst() {
        val mapper = ObjectMapper()
        ip4whitelist!!.init(mapper.readTree("[\"10.0.2.0/24\", \"10.0.0.0/16\"]"))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.10.1"), CoreMatchers.equalTo(true))
    }

    @Test
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun testWhitelistCreationParentFirst() {
        val mapper = ObjectMapper()
        ip4whitelist!!.init(mapper.readTree("[\"10.0.0.0/16\"], \"10.0.2.0/24\""))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.10.1"), CoreMatchers.equalTo(true))
    }

    /* IPv4 validation */
    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badIPv4Input1() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.1/24\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badIPv4Input2() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"256.168.0.1/24\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badNetmaskInput1() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.0.1/33\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badNetmaskInput2() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"::1/129\"]"))
        MatcherAssert.assertThat(badlist.contains("::1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badNetmaskInput3() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.0.1/-1\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun validIPv4Input() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.0.1/32\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun validIPv6Input() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"::1/128\"]"))
        MatcherAssert.assertThat(badlist.contains("::1"), CoreMatchers.equalTo(false))
    }

    /* NetworkNode takes forever to create Tree - commented out until it is needed
	@Test
	public void testAnonymousIpWhitelistPerformance65000() throws NetworkNodeException {
		AnonymousIpWhitelist whitelist = new AnonymousIpWhitelist();
		List<String> tempList = new ArrayList<>();
		// add a bunch of ips to the whitelist

		for (int i = 0; i < 255; i++) {
			for (int j = 0; j < 255; j++) {
				int a = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				int b = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				int c = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				int d = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				tempList.add(String.format("%s.%s.%s.%s", a, b, c, d));
			}
		}

		long startTime = System.nanoTime();

		for (int i = 0; i < tempList.size(); i++) {
			whitelist.add(tempList.get(i) + "/32");
		}

		long durationTime = System.nanoTime() - startTime;

		System.out.println(String.format("Anonymous IP Whitelist creation took %s nanoseconds to create tree of %d subnets", Long.toString(durationTime),
				tempList.size()));

		int total = 1000;

		long start = System.nanoTime();

		for (int i = 0; i <= total; i++) {
			whitelist.contains("192.168.30.1");
		}

		long duration = System.nanoTime() - start;

		System.out.println(
				String.format("Anonymous IP Whitelist average lookup took %s nanoseconds for %d ips", Long.toString(duration / total), tempList.size()));
	}
	*/
    @Test
    @Throws(NetworkNodeException::class)
    fun testAddSubnets() {
        val whitelist = AnonymousIpWhitelist()
        whitelist.add("192.168.1.1/32")
        MatcherAssert.assertThat(whitelist.contains("192.168.1.1"), CoreMatchers.equalTo(true))
        whitelist.add("192.168.1.0/24")
        MatcherAssert.assertThat(whitelist.contains("192.168.1.255"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(whitelist.contains("192.168.1.167"), CoreMatchers.equalTo(true))
        whitelist.add("192.168.1.0/27")
        MatcherAssert.assertThat(whitelist.contains("192.168.1.255"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(whitelist.contains("192.168.1.167"), CoreMatchers.equalTo(true))
        whitelist.add("10.0.0.1/32")
        MatcherAssert.assertThat(whitelist.contains("10.0.0.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(whitelist.contains("10.0.0.2"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.168.2.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.168.2.255"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.167.1.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.169.1.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("10.0.0.0"), CoreMatchers.equalTo(false))
    }
}