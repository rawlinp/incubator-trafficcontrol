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
package com.comcast.cdn.traffic_control.traffic_router.core.dns

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
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
import org.xbill.DNS.Header
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import org.xbill.DNS.Zone
import java.util.ArrayList
import java.util.Collections

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Header::class,
    NameServer::class,
    TrafficRouterManager::class,
    TrafficRouter::class,
    CacheRegister::class
)
class NameServerTest {
    private var nameServer: NameServer? = null
    private var client: InetAddress? = null
    private var trafficRouterManager: TrafficRouterManager? = null
    private var trafficRouter: TrafficRouter? = null
    private var ar: Record? = null
    private var ns: NSRecord? = null
    @Before
    @Throws(Exception::class)
    fun before() {
        client = Inet4Address.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 23, 45))
        nameServer = NameServer()
        trafficRouterManager = Mockito.mock(TrafficRouterManager::class.java)
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        val cacheRegister = Mockito.mock(CacheRegister::class.java)
        Mockito.doReturn(cacheRegister).`when`(trafficRouter)!!.cacheRegister
        val js: JsonNode = JsonNodeFactory.instance.objectNode().put("ecsEnable", true)
        PowerMockito.`when`(cacheRegister.config).thenReturn(js)
        val m_an: Name
        val m_host: Name
        val m_admin: Name
        m_an = Name.fromString("dns1.example.com.")
        m_host = Name.fromString("dns1.example.com.")
        m_admin = Name.fromString("admin.example.com.")
        ar = SOARecord(
            m_an, DClass.IN, 0x13A8,
            m_host, m_admin, 0xABCDEF12L, 0xCDEF1234L,
            0xEF123456L, 0x12345678L, 0x3456789AL
        )
        ns = NSRecord(m_an, DClass.IN, 12345L, m_an)
    }

    @Test
    @Throws(Exception::class)
    fun TestARecordQueryWithClientSubnetOption() {
        val name = Name.fromString("host1.example.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val query = Message.newQuery(question)

        //Add opt record, with client subnet option.
        val nmask = 28
        val ipaddr = Inet4Address.getByName("192.168.33.0")
        val cso = ClientSubnetOption(nmask, ipaddr)
        val cso_list: MutableList<ClientSubnetOption?> = ArrayList(1)
        cso_list.add(cso)
        val opt = OPTRecord(1280, 0, 0, 0, cso_list)
        query.addRecord(opt, Section.ADDITIONAL)


        // Add ARecord Entry in the zone
        val resolvedAddress = Inet4Address.getByName("192.168.8.9")
        val answer: Record = ARecord(name, DClass.IN, 12345L, resolvedAddress)
        val records = arrayOf(ar, ns, answer)
        val m_an = Name.fromString("dns1.example.com.")
        val zone = Zone(m_an, records)
        val builder = DNSAccessRecord.Builder(1L, client)
        nameServer!!.trafficRouterManager = trafficRouterManager
        nameServer!!.setEcsEnable(
            JsonUtils.optBoolean(
                trafficRouter!!.cacheRegister.config,
                "ecsEnable",
                false
            )
        ) // this mimics what happens in ConfigHandler

        // Following is needed to mock this call: zone = trafficRouterManager.getTrafficRouter().getZone(qname, qtype, clientAddress, dnssecRequest, builder);
        PowerMockito.`when`(trafficRouterManager!!.trafficRouter).thenReturn(trafficRouter)
        PowerMockito.`when`(
            trafficRouter!!.getZone(
                Matchers.any(Name::class.java), Matchers.any(
                    Int::class.javaPrimitiveType
                ), Matchers.eq(ipaddr), Matchers.any(
                    Boolean::class.javaPrimitiveType
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenReturn(zone)

        // The function call under test:
        val res = nameServer!!.query(query, client, builder)


        //Verification of response
        val qopt = res.opt!!
        val list: List<EDNSOption> = qopt.getOptions(EDNSOption.Code.CLIENT_SUBNET).filterIsInstance<EDNSOption>()
        assert(list !== Collections.EMPTY_LIST)
        val option = list[0] as ClientSubnetOption?
        MatcherAssert.assertThat(nmask, org.hamcrest.Matchers.equalTo(option!!.sourceNetmask))
        MatcherAssert.assertThat(nmask, org.hamcrest.Matchers.equalTo(option.scopeNetmask))
        MatcherAssert.assertThat(ipaddr, org.hamcrest.Matchers.equalTo(option.address))
        nameServer!!.setEcsEnable(false)
    }

    @Test
    @Throws(Exception::class)
    fun TestARecordQueryWithMultipleClientSubnetOption() {
        val name = Name.fromString("host1.example.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val query = Message.newQuery(question)

        //Add opt record, with multiple client subnet option.
        val nmask1 = 16
        val nmask2 = 24
        val ipaddr1 = Inet4Address.getByName("192.168.0.0")
        val ipaddr2 = Inet4Address.getByName("192.168.33.0")
        val cso1 = ClientSubnetOption(nmask1, ipaddr1)
        val cso2 = ClientSubnetOption(nmask2, ipaddr2)
        val cso_list: MutableList<ClientSubnetOption?> = ArrayList(1)
        cso_list.add(cso1)
        cso_list.add(cso2)
        val opt = OPTRecord(1280, 0, 0, 0, cso_list)
        query.addRecord(opt, Section.ADDITIONAL)


        // Add ARecord Entry in the zone
        val resolvedAddress = Inet4Address.getByName("192.168.8.9")
        val answer: Record = ARecord(name, DClass.IN, 12345L, resolvedAddress)
        val records = arrayOf(ar, ns, answer)
        val m_an = Name.fromString("dns1.example.com.")
        val zone = Zone(m_an, records)
        val builder = DNSAccessRecord.Builder(1L, client)
        nameServer!!.trafficRouterManager = trafficRouterManager
        nameServer!!.setEcsEnable(
            JsonUtils.optBoolean(
                trafficRouter!!.cacheRegister.config,
                "ecsEnable",
                false
            )
        ) // this mimics what happens in ConfigHandler

        // Following is needed to mock this call: zone = trafficRouterManager.getTrafficRouter().getZone(qname, qtype, clientAddress, dnssecRequest, builder);
        PowerMockito.`when`(trafficRouterManager!!.trafficRouter).thenReturn(trafficRouter)
        PowerMockito.`when`(
            trafficRouter!!.getZone(
                Matchers.any(Name::class.java), Matchers.any(
                    Int::class.javaPrimitiveType
                ), Matchers.eq(ipaddr2), Matchers.any(
                    Boolean::class.javaPrimitiveType
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenReturn(zone)

        // The function call under test:
        val res = nameServer!!.query(query, client, builder)


        //Verification of response
        val qopt = res.opt!!
        val list: List<EDNSOption> = qopt.getOptions(EDNSOption.Code.CLIENT_SUBNET).filterIsInstance<EDNSOption>()
        assert(list !== Collections.EMPTY_LIST)
        val option = list[0] as ClientSubnetOption?
        MatcherAssert.assertThat(1, org.hamcrest.Matchers.equalTo(list.size))
        MatcherAssert.assertThat(nmask2, org.hamcrest.Matchers.equalTo(option!!.sourceNetmask))
        MatcherAssert.assertThat(nmask2, org.hamcrest.Matchers.equalTo(option.scopeNetmask))
        MatcherAssert.assertThat(ipaddr2, org.hamcrest.Matchers.equalTo(option.address))
        nameServer!!.setEcsEnable(false)
    }

    @Test
    @Throws(Exception::class)
    fun TestDeliveryServiceARecordQueryWithClientSubnetOption() {
        val cacheRegister = Mockito.mock(CacheRegister::class.java)
        Mockito.doReturn(cacheRegister).`when`(trafficRouter)!!.cacheRegister
        val js: JsonNode = JsonNodeFactory.instance.objectNode().put("ecsEnable", false)
        PowerMockito.`when`(cacheRegister.config).thenReturn(js)
        val node = JsonNodeFactory.instance.objectNode()
        val domainNode = node.putArray("domains")
        domainNode.add("example.com")
        node.put("routingName", "edge")
        node.put("coverageZoneOnly", false)
        val ds1 = DeliveryService("ds1", node)
        val dses: MutableSet<DeliveryService> = HashSet<DeliveryService>()
        dses.add(ds1)
        nameServer!!.setEcsEnabledDses(dses)
        val name = Name.fromString("host1.example.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val query = Message.newQuery(question)

        //Add opt record, with client subnet option.
        val nmask = 28
        val ipaddr = Inet4Address.getByName("192.168.33.0")
        val cso = ClientSubnetOption(nmask, ipaddr)
        val cso_list: MutableList<ClientSubnetOption?> = ArrayList(1)
        cso_list.add(cso)
        val opt = OPTRecord(1280, 0, 0, 0, cso_list)
        query.addRecord(opt, Section.ADDITIONAL)


        // Add ARecord Entry in the zone
        val resolvedAddress = Inet4Address.getByName("192.168.8.9")
        val answer: Record = ARecord(name, DClass.IN, 12345L, resolvedAddress)
        val records = arrayOf(ar, ns, answer)
        val m_an = Name.fromString("dns1.example.com.")
        val zone = Zone(m_an, records)
        val builder = DNSAccessRecord.Builder(1L, client)
        nameServer!!.trafficRouterManager = trafficRouterManager
        nameServer!!.setEcsEnable(
            JsonUtils.optBoolean(
                trafficRouter!!.cacheRegister.config,
                "ecsEnable",
                false
            )
        ) // this mimics what happens in ConfigHandler

        // Following is needed to mock this call: zone = trafficRouterManager.getTrafficRouter().getZone(qname, qtype, clientAddress, dnssecRequest, builder);
        PowerMockito.`when`(trafficRouterManager!!.trafficRouter).thenReturn(trafficRouter)
        PowerMockito.`when`(
            trafficRouter!!.getZone(
                Matchers.any(Name::class.java), Matchers.any(
                    Int::class.javaPrimitiveType
                ), Matchers.eq(ipaddr), Matchers.any(
                    Boolean::class.javaPrimitiveType
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenReturn(zone)

        // The function call under test:
        val res = nameServer!!.query(query, client, builder)


        //Verification of response
        val qopt = res.opt!!
        val list: List<EDNSOption> = qopt.getOptions(EDNSOption.Code.CLIENT_SUBNET).filterIsInstance<EDNSOption>()
        assert(list !== Collections.EMPTY_LIST)
        val option = list[0] as ClientSubnetOption?
        MatcherAssert.assertThat(nmask, org.hamcrest.Matchers.equalTo(option!!.sourceNetmask))
        MatcherAssert.assertThat(nmask, org.hamcrest.Matchers.equalTo(option.scopeNetmask))
        MatcherAssert.assertThat(ipaddr, org.hamcrest.Matchers.equalTo(option.address))
        nameServer!!.setEcsEnable(false)
    }

    @Test
    @Throws(Exception::class)
    fun TestDeliveryServiceARecordQueryWithMultipleClientSubnetOption() {
        val cacheRegister = Mockito.mock(CacheRegister::class.java)
        Mockito.doReturn(cacheRegister).`when`(trafficRouter)!!.cacheRegister
        val js: JsonNode = JsonNodeFactory.instance.objectNode().put("ecsEnable", false)
        PowerMockito.`when`(cacheRegister.config).thenReturn(js)
        val name = Name.fromString("host1.example.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val query = Message.newQuery(question)
        val node = JsonNodeFactory.instance.objectNode()
        val domainNode = node.putArray("domains")
        domainNode.add("example.com")
        node.put("routingName", "edge")
        node.put("coverageZoneOnly", false)
        val ds1 = DeliveryService("ds1", node)
        val dses: MutableSet<DeliveryService> = HashSet<DeliveryService>()
        dses.add(ds1)
        nameServer!!.setEcsEnabledDses(dses)


        //Add opt record, with multiple client subnet option.
        val nmask1 = 16
        val nmask2 = 24
        val ipaddr1 = Inet4Address.getByName("192.168.0.0")
        val ipaddr2 = Inet4Address.getByName("192.168.33.0")
        val cso1 = ClientSubnetOption(nmask1, ipaddr1)
        val cso2 = ClientSubnetOption(nmask2, ipaddr2)
        val cso_list: MutableList<ClientSubnetOption?> = ArrayList(1)
        cso_list.add(cso1)
        cso_list.add(cso2)
        val opt = OPTRecord(1280, 0, 0, 0, cso_list)
        query.addRecord(opt, Section.ADDITIONAL)


        // Add ARecord Entry in the zone
        val resolvedAddress = Inet4Address.getByName("192.168.8.9")
        val answer: Record = ARecord(name, DClass.IN, 12345L, resolvedAddress)
        val records = arrayOf(ar, ns, answer)
        val m_an = Name.fromString("dns1.example.com.")
        val zone = Zone(m_an, records)
        val builder = DNSAccessRecord.Builder(1L, client)
        nameServer!!.trafficRouterManager = trafficRouterManager
        nameServer!!.setEcsEnable(
            JsonUtils.optBoolean(
                trafficRouter!!.cacheRegister.config,
                "ecsEnable",
                false
            )
        ) // this mimics what happens in ConfigHandler

        // Following is needed to mock this call: zone = trafficRouterManager.getTrafficRouter().getZone(qname, qtype, clientAddress, dnssecRequest, builder);
        PowerMockito.`when`(trafficRouterManager!!.trafficRouter).thenReturn(trafficRouter)
        PowerMockito.`when`(
            trafficRouter!!.getZone(
                Matchers.any(Name::class.java), Matchers.any(
                    Int::class.javaPrimitiveType
                ), Matchers.eq(ipaddr2), Matchers.any(
                    Boolean::class.javaPrimitiveType
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenReturn(zone)

        // The function call under test:
        val res = nameServer!!.query(query, client, builder)


        //Verification of response
        val qopt = res.opt!!
        val list: List<EDNSOption> = qopt.getOptions(EDNSOption.Code.CLIENT_SUBNET).filterIsInstance<EDNSOption>()
        assert(list !== Collections.EMPTY_LIST)
        val option = list[0] as ClientSubnetOption?
        MatcherAssert.assertThat(1, org.hamcrest.Matchers.equalTo(list.size))
        MatcherAssert.assertThat(nmask2, org.hamcrest.Matchers.equalTo(option!!.sourceNetmask))
        MatcherAssert.assertThat(nmask2, org.hamcrest.Matchers.equalTo(option.scopeNetmask))
        MatcherAssert.assertThat(ipaddr2, org.hamcrest.Matchers.equalTo(option.address))
        nameServer!!.setEcsEnable(false)
    }
}