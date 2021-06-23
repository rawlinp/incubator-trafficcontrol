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

import com.comcast.cdn.traffic_control.traffic_router.core.edge.InetRecord
import com.comcast.cdn.traffic_control.traffic_router.core.util.CidrAddress
import com.comcast.cdn.traffic_control.traffic_router.core.util.ComparableTreeSet
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test

class FederationRegistryTest {
    private var federations: MutableList<Federation> = ArrayList()

    @Before
    @Throws(Exception::class)
    fun before() {
        val cidrAddress1 = CidrAddress.fromString("192.168.10.11/16")
        val cidrAddress2 = CidrAddress.fromString("192.168.20.22/24")
        val cidrAddressesIpV4 = ComparableTreeSet<CidrAddress>()
        cidrAddressesIpV4.add(cidrAddress1)
        cidrAddressesIpV4.add(cidrAddress2)
        val cidrAddress3 = CidrAddress.fromString("fdfe:dcba:9876:5::/64")
        val cidrAddress4 = CidrAddress.fromString("fd12:3456:789a:1::/64")
        val cidrAddressesIpV6 = ComparableTreeSet<CidrAddress>()
        cidrAddressesIpV6.add(cidrAddress3)
        cidrAddressesIpV6.add(cidrAddress4)
        val federationMapping = FederationMapping("cname1", 1234, cidrAddressesIpV4, cidrAddressesIpV6)
        val federationMappings: MutableList<FederationMapping> = ArrayList()
        federationMappings.add(federationMapping)
        val federation = Federation("kable-town-01", federationMappings)
        federations = ArrayList()
        federations.add(federation)
    }

    @Test
    @Throws(Exception::class)
    fun itFindsMapping() {
        val federationRegistry = FederationRegistry()
        federationRegistry.setFederations(federations)
        var inetRecords =
            federationRegistry.findInetRecords("kable-town-01", CidrAddress.fromString("192.168.10.11/24"))
        MatcherAssert.assertThat(inetRecords, Matchers.containsInAnyOrder(InetRecord("cname1", 1234)))
        inetRecords = federationRegistry.findInetRecords("kable-town-01", CidrAddress.fromString("192.168.10.11/16"))
        MatcherAssert.assertThat(inetRecords, Matchers.containsInAnyOrder(InetRecord("cname1", 1234)))
    }
}