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

import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.xbill.DNS.ARecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Header
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import org.xbill.DNS.WireParseException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.*

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
        Mockito.`when`(client!!.getHostAddress()).thenReturn("192.168.10.11")
        resolver = Mockito.mock(InetAddress::class.java)
        Mockito.`when`(resolver!!.getHostAddress()).thenReturn("10.0.0.211")
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