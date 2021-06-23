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
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.xbill.DNS.DClass
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

@RunWith(PowerMockRunner::class)
@PrepareForTest(AbstractProtocol::class, Message::class)
class TCPTest {
    private var socket: Socket? = null
    private var executorService: ThreadPoolExecutor? = null
    private var cancelService: ExecutorService? = null
    private var queue: LinkedBlockingQueue<Runnable>? = null
    private var nameServer: NameServer? = null
    private var tcp: TCP? = null
    private var client: InetAddress? = null
    private var `in`: ByteArrayInputStream? = null
    private var out: ByteArrayOutputStream? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val serverSocket = Mockito.mock(ServerSocket::class.java)
        socket = Mockito.mock(Socket::class.java)
        executorService = Mockito.mock(ThreadPoolExecutor::class.java)
        cancelService = Mockito.mock(ExecutorService::class.java)
        nameServer = Mockito.mock(NameServer::class.java)
        queue = mock()
        tcp = TCP()
        tcp!!.serverSocket = serverSocket
        tcp!!.executorService = executorService
        tcp!!.cancelService = cancelService
        tcp!!.nameServer = nameServer
        `in` = Mockito.mock(ByteArrayInputStream::class.java)
        client = InetAddress.getLocalHost()
        Mockito.`when`(socket!!.getInetAddress()).thenReturn(client)
        Mockito.`when`(socket!!.getInputStream()).thenReturn(`in`)
        Mockito.`when`(executorService!!.getQueue()).thenReturn(queue)
        Mockito.`when`(queue!!.size).thenReturn(0)
    }

    @Test
    fun testGetMaxResponseLength() {
        Assert.assertEquals(Int.MAX_VALUE.toLong(), tcp!!.getMaxResponseLength(null).toLong())
    }

    @Test
    fun testSubmit() {
        val r = Mockito.mock(
            SocketHandler::class.java
        )
        tcp!!.submit(r)
        Mockito.verify(executorService)!!.submit(r)
    }

    @Test
    @Throws(Exception::class)
    fun testTCPSocketHandler() {
        client = InetAddress.getLocalHost()
        val handler = tcp!!.TCPSocketHandler(socket)
        val name = Name.fromString("www.foo.bar.")
        val question = Record.newRecord(name, Type.A, DClass.IN)
        val request = Message.newQuery(question)
        val wireRequest = request.toWire()
        val requestOut = ByteArrayOutputStream()
        val dos = DataOutputStream(requestOut)
        dos.writeShort(wireRequest.size)
        dos.write(wireRequest)
        `in` = ByteArrayInputStream(requestOut.toByteArray())
        out = ByteArrayOutputStream()
        Mockito.`when`(socket!!.getInputStream()).thenReturn(`in`)
        Mockito.`when`(socket!!.getOutputStream()).thenReturn(out)
        Mockito.`when`(
            nameServer!!.query(
                Matchers.any(
                    Message::class.java
                ), Matchers.eq(client), Matchers.any(
                    DNSAccessRecord.Builder::class.java
                )
            )
        ).thenReturn(request)
        handler.run()
        Assert.assertArrayEquals(requestOut.toByteArray(), out!!.toByteArray())
    }

    @Test
    @Throws(Exception::class)
    fun testTCPSocketHandlerBadMessage() {
        val client = InetAddress.getLocalHost()
        val handler = tcp!!.TCPSocketHandler(socket)
        val wireRequest = ByteArray(0)
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeShort(wireRequest.size)
        dos.write(wireRequest)
        `in` = ByteArrayInputStream(baos.toByteArray())
        Mockito.`when`(socket!!.getInputStream()).thenReturn(`in`)
        val out = ByteArrayOutputStream()
        Mockito.`when`(socket!!.getOutputStream()).thenReturn(out)
        handler.run()
        MatcherAssert.assertThat(out.toByteArray().size, org.hamcrest.Matchers.equalTo(0))
    }

    @Test
    @Throws(Exception::class)
    fun testTCPSocketHandlerQueryFail() {
        val client = InetAddress.getLocalHost()
        val name = Name.fromString("www.foo.bar.")
        val question = Record.newRecord(name, Type.A, DClass.IN)
        val request = Message.newQuery(question)
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeShort(request.toWire().size)
        dos.write(request.toWire())
        `in` = ByteArrayInputStream(baos.toByteArray())
        Mockito.`when`(socket!!.getInputStream()).thenReturn(`in`)
        val response = Message()
        response.header = request.header
        for (i in 0..3) {
            response.removeAllRecords(i)
        }
        response.addRecord(question, Section.QUESTION)
        response.header.rcode = Rcode.SERVFAIL
        val serverFail = response.toWire()
        val expectedResponseOut = ByteArrayOutputStream()
        val dos2 = DataOutputStream(expectedResponseOut)
        dos2.writeShort(serverFail.size)
        dos2.write(serverFail)
        val responseOut = ByteArrayOutputStream()
        Mockito.`when`(socket!!.getOutputStream()).thenReturn(responseOut)
        Mockito.`when`(
            nameServer!!.query(
                Matchers.any(
                    Message::class.java
                ), Matchers.eq(client), Matchers.any(
                    DNSAccessRecord.Builder::class.java
                )
            )
        ).thenThrow(RuntimeException("TCP Query Boom!"))
        val tmp = Message()
        PowerMockito.whenNew(Message::class.java).withParameterTypes(ByteArray::class.java).withArguments(
            Matchers.any(
                ByteArray::class.java
            )
        ).thenReturn(request)
        PowerMockito.whenNew(Message::class.java).withNoArguments().thenReturn(tmp)
        val handler = tcp!!.TCPSocketHandler(socket)
        handler.run()
        Mockito.verify(socket)!!.close()
        val expected = expectedResponseOut.toByteArray()
        val actual = responseOut.toByteArray()
        Assert.assertArrayEquals(expected, actual)
    }
}