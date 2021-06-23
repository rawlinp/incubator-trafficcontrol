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

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouterManager
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtils
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
import org.xbill.DNS.ClientSubnetOption
import org.xbill.DNS.DClass
import org.xbill.DNS.EDNSOption
import org.xbill.DNS.Header
import org.xbill.DNS.Message
import org.xbill.DNS.NSRecord
import org.xbill.DNS.Name
import org.xbill.DNS.OPTRecord
import org.xbill.DNS.Record
import org.xbill.DNS.SOARecord
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import org.xbill.DNS.Zone
import java.net.Inet4Address
import java.net.InetAddress
import java.util.*

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