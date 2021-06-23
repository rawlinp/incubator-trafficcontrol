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
package com.comcast.cdn.traffic_control.traffic_router.core.loc

import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNodeTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.InetAddresses
import org.apache.log4j.Logger
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class NetworkNodeTest {
    private val netMap: Map<String, MutableList<String>> = HashMap()
    private val deepNetMap: MutableMap<String, MutableList<String>> = HashMap()
    private var root: NetworkNode? = null
    private var deepRoot: NetworkNode? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        root = setUp("czmap.json", false)
        deepRoot = setUp("dczmap.json", true)
    }

    @Throws(Exception::class)
    private fun setUp(filename: String, useDeep: Boolean): NetworkNode {
        val testNetMap: MutableMap<String, MutableList<String>> =
            if (useDeep) deepNetMap else netMap as MutableMap<String, MutableList<String>>
        val file = File(javaClass.classLoader.getResource(filename).toURI())
        val nn = NetworkNode.generateTree(file, false, useDeep)
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(file)
        val czKey = if (useDeep) "deepCoverageZones" else "coverageZones"
        val coverageZones = jsonNode[czKey]
        val networkIter = coverageZones.fieldNames()
        while (networkIter.hasNext()) {
            val loc = networkIter.next()
            val locData = coverageZones[loc]
            for (networkType in arrayOf("network", "network6")) {
                val networks = locData[networkType]
                val network = networks[0].asText().split("/".toRegex()).toTypedArray()[0]
                var ip = InetAddresses.forString(network)
                ip = InetAddresses.increment(ip)
                if (!testNetMap.containsKey(loc)) {
                    testNetMap[loc] = ArrayList()
                }
                val addressList = testNetMap[loc]!!
                addressList.add(InetAddresses.toAddrString(ip))
                testNetMap[loc] = addressList
            }
        }
        return nn
    }

    @Test
    fun testIps() {
        try {
            for (location in netMap.keys) {
                for (address in netMap[location]!!) {
                    val nn = root!!.getNetwork(address)
                    Assert.assertNotNull(nn)
                    val loc = nn.loc
                    Assert.assertEquals(loc, location)
                    LOGGER.info(String.format("result for ip=%s: %s", address, loc))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun testNetworkNodePerformance() {
        testNetworkNodePerformance(root, netMap)
    }

    @Test
    fun testDeepNetworkNodePerformance() {
        testNetworkNodePerformance(deepRoot, deepNetMap)
    }

    private fun testNetworkNodePerformance(testRoot: NetworkNode?, testNetMap: Map<String, MutableList<String>>) {
        val iterations = 100000
        val startTime = System.currentTimeMillis()
        val nnTPS = System.getProperty("nnTPS", "12000").toLong()
        for (i in 0 until iterations) {
            for (location in testNetMap.keys) {
                try {
                    for (address in testNetMap[location]!!) {
                        val nn = testRoot!!.getNetwork(address)
                    }
                } catch (e: NetworkNodeException) {
                    e.printStackTrace()
                }
            }
        }
        val runTime = System.currentTimeMillis() - startTime
        val tps = iterations / runTime * 1000
        MatcherAssert.assertThat(tps, Matchers.greaterThanOrEqualTo(nnTPS))
    }

    companion object {
        private val LOGGER = Logger.getLogger(NetworkNodeTest::class.java)
    }
}