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

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test

class NetworkNodeUnitTest {
    @Test
    @Throws(Exception::class)
    fun itSupportsARootNode() {
        val root = NetworkNode("0.0.0.0/0")
        val network = NetworkNode("192.168.1.0/24")
        MatcherAssert.assertThat(root.add(network), Matchers.equalTo(true))
        MatcherAssert.assertThat(root.children.entries.iterator().next().key, Matchers.equalTo(network))
    }

    @Test
    @Throws(Exception::class)
    fun itDoesNotAddANodeOutsideOfNetwork() {
        val network = NetworkNode("192.168.0.0/16")
        val subnetwork = NetworkNode("10.10.0.0/16")
        MatcherAssert.assertThat(network.add(subnetwork), Matchers.equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun itFindsIpBelongingToNetwork() {
        val network = NetworkNode("192.168.1.0/24")
        MatcherAssert.assertThat(network.getNetwork("192.168.1.1"), Matchers.equalTo(network))
        MatcherAssert.assertThat(network.getNetwork("192.168.2.1"), Matchers.not(Matchers.equalTo(network)))
    }

    @Test
    @Throws(Exception::class)
    fun itDoesNotAddDuplicates() {
        val supernet = NetworkNode("192.168.0.0/16")
        val network1 = NetworkNode("192.168.1.0/24")
        val duplicate = NetworkNode("192.168.1.0/24")
        MatcherAssert.assertThat(supernet.add(network1), Matchers.equalTo(true))
        MatcherAssert.assertThat(supernet.children.size, Matchers.equalTo(1))
        MatcherAssert.assertThat(supernet.add(duplicate), Matchers.equalTo(false))
        MatcherAssert.assertThat(supernet.children.size, Matchers.equalTo(1))
    }

    @Test
    @Throws(Exception::class)
    fun itPutsNetworksIntoOrderedHierarchy() {
        val root = NetworkNode("0.0.0.0/0")
        val subnet1 = NetworkNode("192.168.6.0/24")
        val subnet2 = NetworkNode("192.168.55.0/24")
        val net = NetworkNode("192.168.0.0/16")
        root.add(net)
        MatcherAssert.assertThat(root.children.entries.iterator().next().key, Matchers.equalTo(net))
        root.add(subnet2)
        root.add(subnet1)
        val iterator: Iterator<Map.Entry<NetworkNode, NetworkNode>> = net.children.entries.iterator()
        MatcherAssert.assertThat(iterator.next().key, Matchers.equalTo(subnet1))
        MatcherAssert.assertThat(iterator.next().key, Matchers.equalTo(subnet2))
    }

    @Test
    @Throws(Exception::class)
    fun itSupportsDeepCaches() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"deepCoverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"1234:5679::/64\"]," +
                "\"caches\": [\"host1\",\"host2\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        val networkNode = NetworkNode.generateTree(json, false, true)
        val foundNetworkNode = networkNode.getNetwork("192.168.55.100")
        val expected: MutableSet<String> = HashSet()
        expected.add("host1")
        expected.add("host2")
        MatcherAssert.assertThat(foundNetworkNode.deepCacheNames, Matchers.equalTo(expected))
    }

    @Test
    @Throws(Exception::class)
    fun itDoesIpV6() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"1234:5679::/64\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        val networkNode = NetworkNode.generateTree(json, false)
        val foundNetworkNode = networkNode.getNetwork("1234:5678::1")
        MatcherAssert.assertThat(foundNetworkNode.loc, Matchers.equalTo("us-co-denver"))
    }

    @Test
    @Throws(Exception::class)
    fun itPutsAllSubnetsUnderSuperNet() {
        val root = NetworkNode("0.0.0.0/0")
        val subnet1 = NetworkNode("192.168.6.0/24")
        root.add(subnet1)
        val subnet2 = NetworkNode("192.168.55.0/24")
        root.add(subnet2)
        val net = NetworkNode("192.168.0.0/16")
        root.add(net)
        MatcherAssert.assertThat(root.children.isEmpty(), Matchers.equalTo(false))
        val generation1Node = root.children.values.iterator().next()
        MatcherAssert.assertThat(generation1Node.toString(), Matchers.equalTo("[192.168.0.0/16] - location:null"))
        val iterator: Iterator<Map.Entry<NetworkNode, NetworkNode>> = generation1Node.children.entries.iterator()
        val generation2FirstNode = iterator.next().key
        val generation2SecondNode = iterator.next().key
        MatcherAssert.assertThat(generation2FirstNode.toString(), Matchers.equalTo("[192.168.6.0/24] - location:null"))
        MatcherAssert.assertThat(
            generation2SecondNode.toString(),
            Matchers.equalTo("[192.168.55.0/24] - location:null")
        )
    }

    @Test
    @Throws(Exception::class)
    fun itMatchesIpsInOverlappingSubnets() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"0:0:0:0:0:ffff:a4f:3700/24\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        val networkNode = NetworkNode.generateTree(json, false)
        val foundNetworkNode = networkNode.getNetwork("192.168.55.2")
        MatcherAssert.assertThat(foundNetworkNode.loc, Matchers.equalTo("us-co-denver"))
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsInvalidIpV4Network() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/40\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"1234:5679::/64\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        MatcherAssert.assertThat(NetworkNode.generateTree(json, false), Matchers.equalTo(null))
    }

    @Test
    @Throws(Exception::class)
    fun itRejectsInvalidIpV6Network() {
        val czmapString = "{" +
                "\"revision\": \"Mon Dec 21 15:04:01 2015\"," +
                "\"customerName\": \"Kabletown\"," +
                "\"coverageZones\": {" +
                "\"us-co-denver\": {" +
                "\"network\": [\"192.168.55.0/24\",\"192.168.6.0/24\",\"192.168.0.0/16\"]," +
                "\"network6\": [\"1234:5678::/64\",\"zyx:5679::/64\"]" +
                "}" +
                "}" +
                "}"
        val mapper = ObjectMapper()
        val json = mapper.readTree(czmapString)
        MatcherAssert.assertThat(NetworkNode.generateTree(json, false), Matchers.equalTo(null))
    }
}