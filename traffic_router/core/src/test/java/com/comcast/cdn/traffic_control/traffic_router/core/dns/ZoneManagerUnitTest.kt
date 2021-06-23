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

import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouterManager
import com.comcast.cdn.traffic_control.traffic_router.core.util.TrafficOpsUtils
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.xbill.DNS.ARecord
import org.xbill.DNS.DClass
import org.xbill.DNS.NSECRecord
import org.xbill.DNS.Name
import org.xbill.DNS.RRSIGRecord
import org.xbill.DNS.Record
import org.xbill.DNS.SOARecord
import org.xbill.DNS.SetResponse
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type
import org.xbill.DNS.Zone
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(ZoneManager::class, SignatureManager::class)
class ZoneManagerUnitTest {
    var zoneManager: ZoneManager? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        val trafficRouter = Mockito.mock(TrafficRouter::class.java)
        val cacheRegister = Mockito.mock(CacheRegister::class.java)
        Mockito.`when`(trafficRouter.cacheRegister).thenReturn(cacheRegister)
        PowerMockito.spy(ZoneManager::class.java)
        PowerMockito.doNothing().`when`(ZoneManager::class.java, "initTopLevelDomain", cacheRegister)
        PowerMockito.doNothing().`when`(ZoneManager::class.java, "initZoneCache", trafficRouter)
        val signatureManager = PowerMockito.mock(SignatureManager::class.java)
        PowerMockito.whenNew(SignatureManager::class.java).withArguments(
            Matchers.any(
                ZoneManager::class.java
            ), Matchers.any(CacheRegister::class.java), Matchers.any(
                TrafficOpsUtils::class.java
            ), Matchers.any(TrafficRouterManager::class.java)
        ).thenReturn(signatureManager)
        zoneManager = Mockito.spy(
            ZoneManager(
                trafficRouter, StatTracker(), null, Mockito.mock(
                    TrafficRouterManager::class.java
                )
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itMarksResultTypeAndLocationInDNSAccessRecord() {
        val qname = Name.fromString("edge.www.google.com.")
        val client = InetAddress.getByName("192.168.56.78")
        val setResponse = Mockito.mock(SetResponse::class.java)
        Mockito.`when`(setResponse.isSuccessful).thenReturn(false)
        val zone = Mockito.mock(Zone::class.java)
        Mockito.`when`(zone.findRecords(Matchers.any(Name::class.java), Matchers.anyInt())).thenReturn(setResponse)
        Mockito.`when`(zone.origin).thenReturn(Name(qname, 1))
        var builder = DNSAccessRecord.Builder(1L, client)
        builder = Mockito.spy(builder)
        Mockito.doReturn(zone).`when`(zoneManager)!!.getZone(qname, Type.A)
        PowerMockito.doCallRealMethod().`when`(zoneManager)!!.getZone(qname, Type.A, client, false, builder)
        zoneManager!!.getZone(qname, Type.A, client, false, builder)
        Mockito.verify(builder).resultType(
            Matchers.any(
                ResultType::class.java
            )
        )
        Mockito.verify(builder).resultLocation(null)
    }

    @Test
    @Throws(UnknownHostException::class, TextParseException::class)
    fun testZonesAreEqual() {
        class TestCase(var reason: String, var r1: Array<Record>, var r2: Array<Record>, var expected: Boolean)

        val testCases = arrayOf(
            TestCase("empty lists are equal", arrayOf(), arrayOf(), true),
            TestCase(
                "different length lists are unequal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4"))
                ), arrayOf(), false
            ),
            TestCase(
                "same records but different order lists are equal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5"))
                ), arrayOf(
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4"))
                ), true
            ),
            TestCase(
                "same non-empty lists are equal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5"))
                ), arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5"))
                ), true
            ),
            TestCase(
                "lists that only differ in the SOA serial number are equal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        1,
                        60,
                        1,
                        1,
                        1
                    )
                ), arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        2,
                        60,
                        1,
                        1,
                        1
                    )
                ), true
            ),
            TestCase(
                "lists that differ in the SOA (other than the serial number) are not equal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        1,
                        60,
                        1,
                        1,
                        1
                    )
                ), arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        61,
                        Name("example.com."),
                        Name("example.com."),
                        2,
                        60,
                        1,
                        1,
                        1
                    )
                ), false
            ),
            TestCase(
                "lists that only differ in NSEC or RRSIG records are equal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        1,
                        60,
                        1,
                        1,
                        1
                    ),
                    NSECRecord(Name("foo.example.com."), DClass.IN, 60, Name("example.com."), intArrayOf(1)),
                    RRSIGRecord(
                        Name("foo.example.com."),
                        DClass.IN,
                        60,
                        1,
                        1,
                        60,
                        Date(),
                        Date(),
                        1,
                        Name("example.com."),
                        byteArrayOf(1)
                    )
                ), arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        2,
                        60,
                        1,
                        1,
                        1
                    )
                ), true
            ),
            TestCase(
                "lists that only differ in NSEC or RRSIG records are equal", arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        1,
                        60,
                        1,
                        1,
                        1
                    )
                ), arrayOf(
                    ARecord(Name("foo.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.4")),
                    ARecord(Name("bar.example.com."), DClass.IN, 60, InetAddress.getByName("1.2.3.5")),
                    SOARecord(
                        Name("example.com."),
                        DClass.IN,
                        60,
                        Name("example.com."),
                        Name("example.com."),
                        2,
                        60,
                        1,
                        1,
                        1
                    ),
                    NSECRecord(Name("foo.example.com."), DClass.IN, 60, Name("example.com."), intArrayOf(1)),
                    RRSIGRecord(
                        Name("foo.example.com."),
                        DClass.IN,
                        60,
                        1,
                        1,
                        60,
                        Date(),
                        Date(),
                        1,
                        Name("example.com."),
                        byteArrayOf(1)
                    )
                ), true
            )
        )
        for (t in testCases) {
            val input1 = Arrays.asList(*t.r1)
            val input2 = Arrays.asList(*t.r2)
            val copy1 = Arrays.asList(*t.r1)
            val copy2 = Arrays.asList(*t.r2)
            val actual = ZoneManager.zonesAreEqual(input1, input2)
            MatcherAssert.assertThat(t.reason, actual, org.hamcrest.Matchers.equalTo(t.expected))

            // assert that the input lists were not modified
            MatcherAssert.assertThat(
                "zonesAreEqual input lists should not be modified",
                input1,
                org.hamcrest.Matchers.equalTo(copy1)
            )
            MatcherAssert.assertThat(
                "zonesAreEqual input lists should not be modified",
                input2,
                org.hamcrest.Matchers.equalTo(copy2)
            )
        }
    }
}