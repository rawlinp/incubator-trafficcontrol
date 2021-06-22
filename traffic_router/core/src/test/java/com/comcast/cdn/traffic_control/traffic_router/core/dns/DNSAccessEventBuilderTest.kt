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
import org.mockito.Mockito
import org.xbill.DNS.Header
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.util.Random

@RunWith(PowerMockRunner::class)
@PrepareForTest(Random::class, Header::class, DNSAccessEventBuilder::class, System::class, DNSAccessRecord::class)
class DNSAccessEventBuilderTest {
    private var client: InetAddress? = null
    private var resolver: InetAddress? = null
    @Before
    @Throws(Exception::class)
    fun before() {
        PowerMockito.mockStatic(System::class.java)
        val random = Mockito.mock(Random::class.java)
        Mockito.`when`(random.nextInt(0xffff)).thenReturn(65535)
        PowerMockito.whenNew(Random::class.java).withNoArguments().thenReturn(random)
        client = Mockito.mock(InetAddress::class.java)
        Mockito.`when`(client.getHostAddress()).thenReturn("192.168.10.11")
        resolver = Mockito.mock(InetAddress::class.java)
        Mockito.`when`(resolver.getHostAddress()).thenReturn("10.0.0.211")
    }

    @Test
    @Throws(Exception::class)
    fun itCreatesRequestErrorData() {
        Mockito.`when`(System.currentTimeMillis()).thenReturn(144140678789L)
        Mockito.`when`(System.nanoTime()).thenReturn(100000000L, 889000000L)
        val dnsAccessRecord = DNSAccessRecord.Builder(144140678000L, client).build()
        val dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord, WireParseException("invalid record length"))
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=789.000 xn=- fqdn=- type=- class=- rcode=-" +
                        " rtype=- rloc=\"-\" rdtl=- rerr=\"Bad Request:WireParseException:invalid record length\" ttl=\"-\" ans=\"-\""
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itAddsResponseData() {
        val name = Name.fromString("www.example.com.")
        Mockito.`when`(System.nanoTime()).thenReturn(100000000L, 100000000L + 789123000L)
        Mockito.`when`(System.currentTimeMillis()).thenReturn(144140678789L).thenReturn(144140678000L)
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val response = PowerMockito.spy(Message.newQuery(question))
        response.header.rcode = Rcode.NOERROR
        val record1 = Mockito.mock(Record::class.java)
        Mockito.`when`(record1.rdataToString()).thenReturn("foo")
        Mockito.`when`(record1.ttl).thenReturn(1L)
        val record2 = Mockito.mock(Record::class.java)
        Mockito.`when`(record2.rdataToString()).thenReturn("bar")
        Mockito.`when`(record2.ttl).thenReturn(2L)
        val record3 = Mockito.mock(Record::class.java)
        Mockito.`when`(record3.rdataToString()).thenReturn("baz")
        Mockito.`when`(record3.ttl).thenReturn(3L)
        val records = arrayOf(record1, record2, record3)
        Mockito.`when`(response.getSectionArray(Section.ANSWER)).thenReturn(records)
        val answerAddress = Inet4Address.getByName("192.168.1.23")
        val addressRecord = ARecord(name, DClass.IN, 54321L, answerAddress)
        response.addRecord(addressRecord, Section.ANSWER)
        val dnsAccessRecord = DNSAccessRecord.Builder(144140678000L, client).dnsMessage(response).build()
        var dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord)
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=789.123" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=NOERROR rtype=- rloc=\"-\" rdtl=- rerr=\"-\" ttl=\"1 2 3\" ans=\"foo bar baz\""
            )
        )
        Mockito.`when`(System.nanoTime()).thenReturn(100000000L + 456000L)
        dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord)
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=0.456" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=NOERROR rtype=- rloc=\"-\" rdtl=- rerr=\"-\" ttl=\"1 2 3\" ans=\"foo bar baz\""
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itCreatesServerErrorData() {
        val query = Message.newQuery(Record.newRecord(Name.fromString("www.example.com."), Type.A, DClass.IN, 12345L))
        Mockito.`when`(System.currentTimeMillis()).thenReturn(144140678789L)
        Mockito.`when`(System.nanoTime()).thenReturn(100000000L, 100000000L + 789876321L)
        val dnsAccessRecord = DNSAccessRecord.Builder(144140678000L, client).dnsMessage(query).build()
        val dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord, RuntimeException("boom it failed"))
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=789.876" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=SERVFAIL rtype=- rloc=\"-\" rdtl=- rerr=\"Server Error:RuntimeException:boom it failed\" ttl=\"-\" ans=\"-\""
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itAddsResultTypeData() {
        val name = Name.fromString("www.example.com.")
        Mockito.`when`(System.currentTimeMillis()).thenReturn(144140678789L).thenReturn(144140678000L)
        Mockito.`when`(System.nanoTime())
            .thenReturn(100000000L, 100000000L + 789000321L, 100000000L + 123123L, 100000000L + 246001L)
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val response = PowerMockito.spy(Message.newQuery(question))
        response.header.rcode = Rcode.NOERROR
        val record1 = Mockito.mock(Record::class.java)
        Mockito.`when`(record1.rdataToString()).thenReturn("foo")
        Mockito.`when`(record1.ttl).thenReturn(1L)
        val record2 = Mockito.mock(Record::class.java)
        Mockito.`when`(record2.rdataToString()).thenReturn("bar")
        Mockito.`when`(record2.ttl).thenReturn(2L)
        val record3 = Mockito.mock(Record::class.java)
        Mockito.`when`(record3.rdataToString()).thenReturn("baz")
        Mockito.`when`(record3.ttl).thenReturn(3L)
        val records = arrayOf(record1, record2, record3)
        Mockito.`when`(response.getSectionArray(Section.ANSWER)).thenReturn(records)
        val answerAddress = Inet4Address.getByName("192.168.1.23")
        val addressRecord = ARecord(name, DClass.IN, 54321L, answerAddress)
        response.addRecord(addressRecord, Section.ANSWER)
        val resultLocation = Geolocation(39.7528, -104.9997)
        val resultType = ResultType.CZ
        val builder = DNSAccessRecord.Builder(144140678000L, client)
            .dnsMessage(response).resultType(resultType).resultLocation(resultLocation)
        var dnsAccessRecord = builder.build()
        var dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord)
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=789.000" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=NOERROR rtype=CZ rloc=\"39.75,-104.99\" rdtl=- rerr=\"-\" ttl=\"1 2 3\" ans=\"foo bar baz\""
            )
        )
        dnsAccessRecord = builder.resultType(ResultType.GEO).build()
        dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord)
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=0.123" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=NOERROR rtype=GEO rloc=\"39.75,-104.99\" rdtl=- rerr=\"-\" ttl=\"1 2 3\" ans=\"foo bar baz\""
            )
        )
        dnsAccessRecord = builder.resultType(ResultType.MISS).resultDetails(ResultDetails.DS_NOT_FOUND).build()
        dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord)
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=- ttms=0.246" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=NOERROR rtype=MISS rloc=\"39.75,-104.99\" rdtl=DS_NOT_FOUND rerr=\"-\" ttl=\"1 2 3\" ans=\"foo bar baz\""
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itLogsResolverAndClient() {
        val name = Name.fromString("www.example.com.")
        Mockito.`when`(System.currentTimeMillis()).thenReturn(144140678789L).thenReturn(144140678000L)
        Mockito.`when`(System.nanoTime())
            .thenReturn(100000000L, 100000000L + 789000321L, 100000000L + 123123L, 100000000L + 246001L)
        val question = Record.newRecord(name, Type.A, DClass.IN, 12345L)
        val response = PowerMockito.spy(Message.newQuery(question))
        response.header.rcode = Rcode.NOERROR
        val record1 = Mockito.mock(Record::class.java)
        Mockito.`when`(record1.rdataToString()).thenReturn("foo")
        Mockito.`when`(record1.ttl).thenReturn(1L)
        val records = arrayOf(record1)
        Mockito.`when`(response.getSectionArray(Section.ANSWER)).thenReturn(records)
        val answerAddress = Inet4Address.getByName("192.168.1.23")
        val addressRecord = ARecord(name, DClass.IN, 54321L, answerAddress)
        response.addRecord(addressRecord, Section.ANSWER)
        val resultLocation = Geolocation(39.7528, -104.9997)
        val resultType = ResultType.CZ
        val builder = DNSAccessRecord.Builder(144140678000L, resolver)
            .dnsMessage(response).resultType(resultType).resultLocation(resultLocation).client(client)
        val dnsAccessRecord = builder.build()
        val dnsAccessEvent = DNSAccessEventBuilder.create(dnsAccessRecord)
        MatcherAssert.assertThat(
            dnsAccessEvent, Matchers.equalTo(
                "144140678.000 qtype=DNS chi=192.168.10.11 rhi=10.0.0.211 ttms=789.000" +
                        " xn=65535 fqdn=www.example.com. type=A class=IN" +
                        " rcode=NOERROR rtype=CZ rloc=\"39.75,-104.99\" rdtl=- rerr=\"-\" ttl=\"1\" ans=\"foo\""
            )
        )
    }
}