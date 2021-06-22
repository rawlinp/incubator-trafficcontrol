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
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Matchers
import org.mockito.Mockito
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.Type
import java.util.ArrayList
import java.util.Date

@RunWith(PowerMockRunner::class)
@PrepareForTest(ZoneSignerImpl::class)
class ZoneSignerImplTest {
    internal class IsRRsetTypeA : ArgumentMatcher<RRset?>() {
        override fun matches(rrset: Any): Boolean {
            return (rrset as RRset).type == Type.A
        }
    }

    internal class IsRRsetTypeNSEC : ArgumentMatcher<RRset?>() {
        override fun matches(rrset: Any): Boolean {
            return (rrset as RRset).type == Type.NSEC
        }
    }

    @Test
    @Throws(Exception::class)
    fun signZoneWithRRSIGCacheTest() {
        val zoneSigner = PowerMockito.spy(ZoneSignerImpl())
        val records: MutableList<Record> = ArrayList()
        val ARecord1: Record = ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4"))
        val ARecord2: Record = ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5"))
        val ARecord3: Record = ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.6"))
        val ARecord4: Record = ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.7"))
        val zskPair: DnsSecKeyPair = Mockito.mock(DnsSecKeyPairImpl::class.java)
        val zskDnskey = Mockito.mock(DNSKEYRecord::class.java)
        Mockito.`when`(zskPair.dnskeyRecord).thenReturn(zskDnskey)
        val zskKey = Mockito.mock(PrivateKey::class.java)
        Mockito.`when`(zskPair.private).thenReturn(zskKey)
        Mockito.`when`(zskKey.encoded).thenReturn(byteArrayOf(1))
        Mockito.`when`(zskDnskey.algorithm).thenReturn(1)
        val kskPairs: List<DnsSecKeyPair> = ArrayList()
        val zskPairs = listOf(zskPair)
        val inception = Date()
        val expire = Date.from(inception.toInstant().plusSeconds(100000))
        val aRRSigRecord = RRSIGRecord(
            Name("foo.example.com."),
            DClass.IN,
            60,
            Type.A,
            1,
            60,
            inception,
            expire,
            1,
            Name("example.com."),
            byteArrayOf(1)
        )
        val nsecRRSigRecord = RRSIGRecord(
            Name("foo.example.com."),
            DClass.IN,
            60,
            Type.NSEC,
            1,
            60,
            inception,
            expire,
            1,
            Name("example.com."),
            byteArrayOf(2)
        )
        PowerMockito.doReturn(aRRSigRecord).`when`(
            zoneSigner, "sign", Matchers.argThat(IsRRsetTypeA()), Matchers.any(
                DNSKEYRecord::class.java
            ), Matchers.any(PrivateKey::class.java), Matchers.eq(inception), Matchers.eq(expire)
        )
        PowerMockito.doReturn(nsecRRSigRecord).`when`(
            zoneSigner, "sign", Matchers.argThat(IsRRsetTypeNSEC()), Matchers.any(
                DNSKEYRecord::class.java
            ), Matchers.any(PrivateKey::class.java), Matchers.eq(inception), Matchers.eq(expire)
        )
        val newInception = Date.from(inception.toInstant().plusSeconds(100))
        val newExpire = Date.from(newInception.toInstant().plusSeconds(100000))
        val newARRSigRecord = RRSIGRecord(
            Name("foo.example.com."),
            DClass.IN,
            60,
            Type.A,
            1,
            60,
            newInception,
            newExpire,
            1,
            Name("example.com."),
            byteArrayOf(3)
        )
        val newNSECRRSigRecord = RRSIGRecord(
            Name("foo.example.com."),
            DClass.IN,
            60,
            Type.NSEC,
            1,
            60,
            newInception,
            newExpire,
            1,
            Name("example.com."),
            byteArrayOf(4)
        )
        PowerMockito.doReturn(newARRSigRecord).`when`(
            zoneSigner, "sign", Matchers.argThat(IsRRsetTypeA()), Matchers.any(
                DNSKEYRecord::class.java
            ), Matchers.any(PrivateKey::class.java), Matchers.eq(newInception), Matchers.eq(newExpire)
        )
        PowerMockito.doReturn(newNSECRRSigRecord).`when`(
            zoneSigner, "sign", Matchers.argThat(IsRRsetTypeNSEC()), Matchers.any(
                DNSKEYRecord::class.java
            ), Matchers.any(PrivateKey::class.java), Matchers.eq(newInception), Matchers.eq(newExpire)
        )
        val expiresSoonInception = Date.from(inception.toInstant().minusSeconds(100))
        val expiresSoonExpire = Date.from(inception.toInstant().plusSeconds(50))
        val expiresSoonARRSigRecord = RRSIGRecord(
            Name("foo.example.com."),
            DClass.IN,
            60,
            Type.A,
            1,
            60,
            expiresSoonInception,
            expiresSoonExpire,
            1,
            Name("example.com."),
            byteArrayOf(5)
        )
        val expiresSoonNSECRRSigRecord = RRSIGRecord(
            Name("foo.example.com."),
            DClass.IN,
            60,
            Type.NSEC,
            1,
            60,
            expiresSoonInception,
            expiresSoonExpire,
            1,
            Name("example.com."),
            byteArrayOf(6)
        )
        PowerMockito.doReturn(expiresSoonARRSigRecord).`when`(
            zoneSigner, "sign", Matchers.argThat(IsRRsetTypeA()), Matchers.any(
                DNSKEYRecord::class.java
            ), Matchers.any(PrivateKey::class.java), Matchers.eq(expiresSoonInception), Matchers.eq(expiresSoonExpire)
        )
        PowerMockito.doReturn(expiresSoonNSECRRSigRecord).`when`(
            zoneSigner, "sign", Matchers.argThat(IsRRsetTypeNSEC()), Matchers.any(
                DNSKEYRecord::class.java
            ), Matchers.any(PrivateKey::class.java), Matchers.eq(expiresSoonInception), Matchers.eq(expiresSoonExpire)
        )
        val RRSIGCache: ConcurrentMap<RRSIGCacheKey, ConcurrentMap<RRsetKey, RRSIGRecord>> = ConcurrentHashMap()
        records.add(ARecord1)
        records.add(ARecord2)
        var signedRecords = zoneSigner.signZone(records, kskPairs, zskPairs, inception, expire, RRSIGCache)
        var ret = signedRecords.stream().filter { r: Record? -> r is RRSIGRecord && r.typeCovered == Type.A }
            .findFirst().orElse(null) as RRSIGRecord
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.notNullValue())
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.equalTo(aRRSigRecord))

        // re-signing the same RRset with new timestamps should reuse the cached RRSIG record
        records.clear()
        records.add(ARecord1)
        records.add(ARecord2)
        signedRecords = zoneSigner.signZone(records, kskPairs, zskPairs, newInception, newExpire, RRSIGCache)
        ret = signedRecords.stream().filter { r: Record? -> r is RRSIGRecord && r.typeCovered == Type.A }
            .findFirst().orElse(null) as RRSIGRecord
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.notNullValue())
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.equalTo(aRRSigRecord))

        // changed RRset should be re-signed
        records.clear()
        records.add(ARecord1)
        records.add(ARecord2)
        records.add(ARecord3)
        records.add(ARecord4)
        signedRecords = zoneSigner.signZone(records, kskPairs, zskPairs, newInception, newExpire, RRSIGCache)
        ret = signedRecords.stream().filter { r: Record? -> r is RRSIGRecord && r.typeCovered == Type.A }
            .findFirst().orElse(null) as RRSIGRecord
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.notNullValue())
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.equalTo(newARRSigRecord))

        // re-signing 1st RRset again should reuse the cached RRSIG record
        records.clear()
        records.add(ARecord1)
        records.add(ARecord2)
        signedRecords = zoneSigner.signZone(records, kskPairs, zskPairs, newInception, newExpire, RRSIGCache)
        ret = signedRecords.stream().filter { r: Record? -> r is RRSIGRecord && r.typeCovered == Type.A }
            .findFirst().orElse(null) as RRSIGRecord
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.notNullValue())
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.equalTo(aRRSigRecord))

        // re-signing RRset that has a cached RRSIG record that is close to expiring should be re-signed
        records.clear()
        records.add(ARecord3)
        records.add(ARecord4)
        signedRecords =
            zoneSigner.signZone(records, kskPairs, zskPairs, expiresSoonInception, expiresSoonExpire, RRSIGCache)
        ret = signedRecords.stream().filter { r: Record? -> r is RRSIGRecord && r.typeCovered == Type.A }
            .findFirst().orElse(null) as RRSIGRecord
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.notNullValue())
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.equalTo(expiresSoonARRSigRecord))
        records.clear()
        records.add(ARecord3)
        records.add(ARecord4)
        signedRecords = zoneSigner.signZone(records, kskPairs, zskPairs, newInception, newExpire, RRSIGCache)
        ret = signedRecords.stream().filter { r: Record? -> r is RRSIGRecord && r.typeCovered == Type.A }
            .findFirst().orElse(null) as RRSIGRecord
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.notNullValue())
        MatcherAssert.assertThat(ret, org.hamcrest.Matchers.equalTo(newARRSigRecord))
    }
}