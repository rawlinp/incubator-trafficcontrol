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

import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSAccessRecord
import com.comcast.cdn.traffic_control.traffic_router.core.dns.NameServer
import com.nhaarman.mockitokotlin2.mock
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.xbill.DNS.DClass
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.OPTRecord
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicInteger

@RunWith(PowerMockRunner::class)
@PrepareForTest(AbstractProtocol::class, Message::class)
class UDPTest {
    private var datagramSocket: DatagramSocket? = null
    private var executorService: ThreadPoolExecutor? = null
    private var cancelService: ExecutorService? = null
    private var queue: LinkedBlockingQueue<Runnable>? = null
    private var nameServer: NameServer? = null
    private var udp: UDP? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        datagramSocket = Mockito.mock(DatagramSocket::class.java)
        executorService = Mockito.mock(ThreadPoolExecutor::class.java)
        cancelService = Mockito.mock(ExecutorService::class.java)
        queue = mock()
        nameServer = Mockito.mock(NameServer::class.java)
        udp = UDP()
        udp!!.datagramSocket = datagramSocket
        udp!!.executorService = executorService
        udp!!.cancelService = cancelService
        udp!!.nameServer = nameServer
        Mockito.`when`(executorService!!.getQueue()).thenReturn(queue)
        Mockito.`when`(queue!!.size).thenReturn(0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMaxResponseLengthNoOPTQuery() {
        val name = Name.fromString("www.foo.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN)
        val request = Message.newQuery(question)
        Assert.assertEquals(512, udp!!.getMaxResponseLength(request).toLong())
    }

    @Test
    fun testGetMaxResponseLengthNullQuery() {
        Assert.assertEquals(512, udp!!.getMaxResponseLength(null).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetMaxResponseLengthWithOPTQuery() {
        val size = 1280
        val name = Name.fromString("www.foo.com.")
        val question = Record.newRecord(name, Type.A, DClass.IN)
        val options = OPTRecord(size, 0, 0)
        val request = Message.newQuery(question)
        request.addRecord(options, Section.ADDITIONAL)
        Assert.assertEquals(size.toLong(), udp!!.getMaxResponseLength(request).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSubmit() {
        val r = Mockito.mock(
            SocketHandler::class.java
        )
        udp!!.submit(r)
        Mockito.verify(executorService)!!.submit(r)
    }

    @Test
    @Throws(Exception::class)
    fun testUDPPacketHandler() {
        val client = InetAddress.getLocalHost()
        val port = 11111
        val name = Name.fromString("www.foo.bar.")
        val question = Record.newRecord(name, Type.A, DClass.IN)
        val request = Message.newQuery(question)
        val wireRequest = request.toWire()
        val aRecord = Record.newRecord(name, Type.A, DClass.IN, 3600)
        val response = Message.newQuery(question)
        response.header.setFlag(Flags.QR.toInt())
        response.addRecord(aRecord, Section.ANSWER)
        val wireResponse = response.toWire()
        val packet = DatagramPacket(wireRequest, wireRequest.size, client, port)
        Mockito.`when`(
            nameServer!!.query(
                Matchers.any(
                    Message::class.java
                ), Matchers.eq(client), Matchers.any(
                    DNSAccessRecord.Builder::class.java
                )
            )
        ).thenReturn(response)
        val count = AtomicInteger(0)
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val datagramPacket = invocation.arguments[0] as DatagramPacket
            MatcherAssert.assertThat(datagramPacket.data, org.hamcrest.Matchers.equalTo(wireResponse))
            count.incrementAndGet()
            null
        }.`when`(datagramSocket)!!.send(Matchers.any(DatagramPacket::class.java))
        val handler = udp!!.UDPPacketHandler(packet)
        handler.run()
        MatcherAssert.assertThat(count.get(), org.hamcrest.Matchers.equalTo(1))
    }

    @Test
    @Throws(Exception::class)
    fun testUDPPacketHandlerBadMessage() {
        val client = InetAddress.getLocalHost()
        val port = 11111
        val wireRequest = ByteArray(0)
        val packet = DatagramPacket(wireRequest, wireRequest.size, client, port)
        val handler = udp!!.UDPPacketHandler(packet)
        handler.run()
    }

    @Test
    @Throws(Exception::class)
    fun testUDPPacketHandlerQueryFail() {
        val client = InetAddress.getLocalHost()
        val port = 11111
        val name = Name.fromString("www.foo.bar.")
        val question = Record.newRecord(name, Type.A, DClass.IN)
        val request = Message.newQuery(question)
        val wireRequest = request.toWire()
        val response = Message()
        response.header = request.header
        for (i in 0..3) {
            response.removeAllRecords(i)
        }
        response.addRecord(question, Section.QUESTION)
        response.header.rcode = Rcode.SERVFAIL
        val wireResponse = response.toWire()
        val packet = DatagramPacket(wireRequest, wireRequest.size, client, port)
        val count = AtomicInteger(0)
        Mockito.`when`(
            nameServer!!.query(
                Matchers.any(
                    Message::class.java
                ), Matchers.eq(client), Matchers.any(
                    DNSAccessRecord.Builder::class.java
                )
            )
        ).thenThrow(RuntimeException("Boom! UDP Query"))
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val datagramPacket = invocation.arguments[0] as DatagramPacket
            MatcherAssert.assertThat(datagramPacket.data, org.hamcrest.Matchers.equalTo(wireResponse))
            count.incrementAndGet()
            null
        }.`when`(datagramSocket)!!.send(Matchers.any(DatagramPacket::class.java))
        val handler = udp!!.UDPPacketHandler(packet)
        handler.run()
        MatcherAssert.assertThat(count.get(), org.hamcrest.Matchers.equalTo(1))
    }
}