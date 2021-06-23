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
package com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol

import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSAccessEventBuilder
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSAccessRecord
import com.comcast.cdn.traffic_control.traffic_router.core.dns.NameServer
import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.AbstractProtocolTest.FakeAbstractProtocol
import org.apache.log4j.Logger
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
import org.xbill.DNS.Flags
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
@PrepareForTest(
    FakeAbstractProtocol::class,
    Logger::class,
    DNSAccessEventBuilder::class,
    Header::class,
    NameServer::class,
    DNSAccessRecord::class
)
class AbstractProtocolTest {
    private var nameServer: NameServer? = null
    private var header: Header? = null
    var client: InetAddress? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        // force the xn field in the request
        val random = Mockito.mock(Random::class.java)
        Mockito.`when`(random.nextInt(0xffff)).thenReturn(65535)
        PowerMockito.whenNew(Random::class.java).withNoArguments().thenReturn(random)
        PowerMockito.mockStatic(System::class.java)
        PowerMockito.`when`(System.currentTimeMillis()).thenReturn(144140678000L).thenReturn(144140678345L)
        PowerMockito.`when`(System.nanoTime()).thenReturn(100000000L, 100000000L + 345123000L)
        PowerMockito.mockStatic(Logger::class.java)
        PowerMockito.`when`(Logger.getLogger("com.comcast.cdn.traffic_control.traffic_router.core.access")).thenReturn(
            accessLogger
        )
        header = Header()
        header!!.id = 65535
        header!!.setFlag(Flags.QR.toInt())
        client = Inet4Address.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 23, 45))
        nameServer = Mockito.mock(NameServer::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun itLogsARecordQueries() {
        header!!.rcode = Rcode.NOERROR
        val name = Name.fromString("www.example.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN, 0L)
        val query = Message.newQuery(question)
        query.header.rcode
        val queryBytes = query.toWire()
        PowerMockito.whenNew(Message::class.java).withArguments(queryBytes).thenReturn(query)
        val resolvedAddress = Inet4Address.getByName("192.168.8.9")
        val answer: Record = ARecord(name, DClass.IN, 3600L, resolvedAddress)
        val answers = arrayOf(answer)
        val response = Mockito.mock(Message::class.java)
        PowerMockito.`when`(response.header).thenReturn(header)
        PowerMockito.`when`(response.getSectionArray(Section.ANSWER)).thenReturn(answers)
        PowerMockito.`when`(response.question).thenReturn(question)
        val client = Inet4Address.getByName("192.168.23.45")
        PowerMockito.`when`(
            nameServer!!.query(
                Matchers.any(Message::class.java), Matchers.any(
                    InetAddress::class.java
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenReturn(response)
        val abstractProtocol = FakeAbstractProtocol(client, queryBytes)
        abstractProtocol.nameServer = nameServer
        abstractProtocol.run()
        Mockito.verify(accessLogger)
            .info("144140678.000 qtype=DNS chi=192.168.23.45 rhi=- ttms=345.123 xn=65535 fqdn=www.example.com. type=A class=IN rcode=NOERROR rtype=- rloc=\"-\" rdtl=- rerr=\"-\" ttl=\"3600\" ans=\"192.168.8.9\"")
    }

    @Test
    @Throws(Exception::class)
    fun itLogsOtherQueries() {
        header!!.rcode = Rcode.REFUSED
        val name = Name.fromString("John Wayne.")
        val question = Record.newRecord(name, 65530, 43210)
        val query = Message.newQuery(question)
        val response = Mockito.mock(Message::class.java)
        PowerMockito.`when`(response.header).thenReturn(header)
        PowerMockito.`when`(response.getSectionArray(Section.ANSWER)).thenReturn(null)
        PowerMockito.`when`(response.question).thenReturn(question)
        PowerMockito.`when`(
            nameServer!!.query(
                Matchers.any(Message::class.java), Matchers.any(
                    InetAddress::class.java
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenReturn(response)
        val abstractProtocol = FakeAbstractProtocol(client, query.toWire())
        abstractProtocol.nameServer = nameServer
        abstractProtocol.run()
        Mockito.verify(accessLogger)
            .info("144140678.000 qtype=DNS chi=192.168.23.45 rhi=- ttms=345.123 xn=65535 fqdn=John\\032Wayne. type=TYPE65530 class=CLASS43210 rcode=REFUSED rtype=- rloc=\"-\" rdtl=- rerr=\"-\" ttl=\"-\" ans=\"-\"")
    }

    @Test
    @Throws(Exception::class)
    fun itLogsBadClientRequests() {
        val abstractProtocol = FakeAbstractProtocol(client, byteArrayOf(1, 2, 3, 4, 5, 6, 7))
        abstractProtocol.nameServer = nameServer
        abstractProtocol.run()
        Mockito.verify(accessLogger)
            .info("144140678.000 qtype=DNS chi=192.168.23.45 rhi=- ttms=345.123 xn=- fqdn=- type=- class=- rcode=- rtype=- rloc=\"-\" rdtl=- rerr=\"Bad Request:WireParseException:end of input\" ttl=\"-\" ans=\"-\"")
    }

    @Test
    @Throws(Exception::class)
    fun itLogsServerErrors() {
        header!!.rcode = Rcode.REFUSED
        val name = Name.fromString("John Wayne.")
        val question = Record.newRecord(name, 65530, 43210)
        val query = Message.newQuery(question)
        val response = Mockito.mock(Message::class.java)
        PowerMockito.`when`(response.header).thenReturn(header)
        PowerMockito.`when`(response.getSectionArray(Section.ANSWER)).thenReturn(null)
        PowerMockito.`when`(response.question).thenReturn(question)
        PowerMockito.`when`(
            nameServer!!.query(
                Matchers.any(Message::class.java), Matchers.any(
                    InetAddress::class.java
                ), Matchers.any(DNSAccessRecord.Builder::class.java)
            )
        ).thenThrow(RuntimeException("Aw snap!"))
        val abstractProtocol = FakeAbstractProtocol(client, query.toWire())
        abstractProtocol.nameServer = nameServer
        abstractProtocol.run()
        Mockito.verify(accessLogger)
            .info("144140678.000 qtype=DNS chi=192.168.23.45 rhi=- ttms=345.123 xn=65535 fqdn=John\\032Wayne. type=TYPE65530 class=CLASS43210 rcode=SERVFAIL rtype=- rloc=\"-\" rdtl=- rerr=\"Server Error:RuntimeException:Aw snap!\" ttl=\"-\" ans=\"-\"")
    }

    inner class FakeAbstractProtocol(private val inetAddress: InetAddress?, private val request: ByteArray) :
        AbstractProtocol() {
        override fun getMaxResponseLength(request: Message): Int {
            return Int.MAX_VALUE
        }

        override fun run() {
            try {
                query(inetAddress, request)
            } catch (e: WireParseException) {
                // Ignore it
            }
        }
    }

    companion object {
        private val accessLogger = Mockito.mock(Logger::class.java)
    }
}