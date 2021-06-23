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

import com.comcast.cdn.traffic_control.traffic_router.core.TestBase
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node.IPVersions
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouterManager
import com.comcast.cdn.traffic_control.traffic_router.core.util.IntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.InetAddresses
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.core.IsCollectionContaining
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.springframework.context.ApplicationContext
import org.xbill.DNS.Name
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type
import org.xbill.DNS.Zone
import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Paths

@Category(IntegrationTest::class)
class ZoneManagerTest {
    private var trafficRouterManager: TrafficRouterManager? = null
    private val netMap: MutableMap<String, InetAddress> = HashMap()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        trafficRouterManager = context!!.getBean("trafficRouterManager") as TrafficRouterManager
        trafficRouterManager!!.trafficRouter.setApplicationContext(context)
        val file = File("src/test/db/czmap.json")
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(file)
        val coverageZones = jsonNode["coverageZones"]
        val czIter = coverageZones.fieldNames()
        while (czIter.hasNext()) {
            val loc = czIter.next()
            val locData = coverageZones[loc]
            val networks = locData["network"]
            val network = networks[0].asText().split("/".toRegex()).toTypedArray()[0]
            var ip = InetAddresses.forString(network)
            ip = InetAddresses.increment(ip)
            netMap[loc] = ip
        }
    }

    @Test
    @Throws(TextParseException::class)
    fun testDynamicZoneCache() {
        val trafficRouter = trafficRouterManager!!.trafficRouter
        val cacheRegister = trafficRouter.cacheRegister
        val zoneManager = trafficRouter.zoneManager
        for (ds in cacheRegister.deliveryServices.values) {
            if (!ds.isDns) {
                continue
            }
            val domain = ds.domain
            val edgeName = Name(ds.routingName + "." + domain + ".")
            for (source in netMap.values) {
                val location = trafficRouter.getCoverageZoneCacheLocation(source.hostAddress, ds, IPVersions.IPV4ONLY)
                val caches = trafficRouter.selectCachesByCZ(ds, location, IPVersions.IPV4ONLY) ?: continue
                val builder = DNSAccessRecord.Builder(1, source)
                val zones: MutableSet<Zone?> = HashSet()
                val maxDnsIps = ds.maxDnsIps
                var combinations: Long = 1
                if (maxDnsIps > 0 && !trafficRouter.isConsistentDNSRouting && caches.size > maxDnsIps) {
                    val top = fact(caches.size)
                    val f = fact(caches.size - maxDnsIps)
                    val s = fact(maxDnsIps)
                    combinations = top.divide(f.multiply(s)).toLong()
                    var c = 0
                    while (c < combinations * 100) {
                        val zone = trafficRouter.getZone(
                            edgeName,
                            Type.A,
                            source,
                            true,
                            builder
                        ) // this should load the zone into the dynamicZoneCache if not already there
                        Assert.assertNotNull(zone)
                        zones.add(zone)
                        c++
                    }
                }
                val cacheStats = zoneManager.dynamicCacheStats
                for (j in 0..combinations * 100) {
                    val missCount = cacheStats.missCount()
                    val zone = trafficRouter.getZone(edgeName, Type.A, source, true, builder)
                    Assert.assertNotNull(zone)
                    Assert.assertEquals(
                        missCount,
                        cacheStats.missCount()
                    ) // should always be a cache hit so these should remain the same
                    if (!zones.isEmpty()) {
                        MatcherAssert.assertThat<Set<Zone?>>(zones, IsCollectionContaining.hasItem(zone))
                        Assert.assertTrue(zones.contains(zone))
                    }
                }
            }
        }
    }

    private fun fact(n: Int): BigInteger {
        var p = BigInteger("1")
        for (c in n downTo 1) {
            p = p.multiply(BigInteger.valueOf(c.toLong()))
        }
        return p
    }

    companion object {
        private var context: ApplicationContext? = null

        @BeforeClass
        @Throws(Exception::class)
        fun setUpBeforeClass() {
            MatcherAssert.assertThat(
                "Copy core/src/main/conf/traffic_monitor.properties to core/src/test/conf and set 'traffic_monitor.bootstrap.hosts' to a real traffic monitor",
                Files.exists(Paths.get(TestBase.monitorPropertiesPath)),
                Matchers.equalTo(true)
            )
            context = TestBase.context
        }
    }
}