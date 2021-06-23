/*
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

import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNodeException
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtilsException
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AnonymousIpWhitelistTest {
    var ip4whitelist: AnonymousIpWhitelist? = null
    var ip6whitelist: AnonymousIpWhitelist? = null

    @Before
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun setup() {
        val mapper = ObjectMapper()
        ip4whitelist = AnonymousIpWhitelist()
        ip4whitelist!!.init(mapper.readTree("[\"192.168.30.0/24\", \"10.0.2.0/24\", \"10.0.0.0/16\"]"))
        ip6whitelist = AnonymousIpWhitelist()
        ip6whitelist!!.init(mapper.readTree("[\"::1/32\", \"2001::/64\"]"))
    }

    @Test
    fun testAnonymousIpWhitelistConstructor() {
        // final InetAddress address = InetAddresses.forString("192.168.30.1");
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.30.1"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIPsInWhitelist() {
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.30.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.30.254"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.254"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.1.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.254.254"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIPsNotInWhitelist() {
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.168.31.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip4whitelist!!.contains("192.167.30.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.1.1.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.10.1.1"), CoreMatchers.equalTo(false))
    }

    /* IPv6 Testing */
    @Test
    fun testIPv6AddressInWhitelist() {
        MatcherAssert.assertThat(ip6whitelist!!.contains("::1"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIPv6AddressInWhitelistInSubnet() {
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001::"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:0:0:0:1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:0:0:1:1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:a:a:a:a"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:0:ffff:ffff:ffff:ffff"), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIpv6AddressNotInWhitelist() {
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:1:0:0:0:0:0:0"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:1::"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2002:0:0:0:0:0:0:1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(ip6whitelist!!.contains("2001:0:0:1:ffff:ffff:ffff:ffff"), CoreMatchers.equalTo(false))
    }

    @Test
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun testWhitelistCreationLeafFirst() {
        val mapper = ObjectMapper()
        ip4whitelist!!.init(mapper.readTree("[\"10.0.2.0/24\", \"10.0.0.0/16\"]"))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.10.1"), CoreMatchers.equalTo(true))
    }

    @Test
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun testWhitelistCreationParentFirst() {
        val mapper = ObjectMapper()
        ip4whitelist!!.init(mapper.readTree("[\"10.0.0.0/16\"], \"10.0.2.0/24\""))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.2.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(ip4whitelist!!.contains("10.0.10.1"), CoreMatchers.equalTo(true))
    }

    /* IPv4 validation */
    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badIPv4Input1() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.1/24\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badIPv4Input2() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"256.168.0.1/24\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badNetmaskInput1() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.0.1/33\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badNetmaskInput2() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"::1/129\"]"))
        MatcherAssert.assertThat(badlist.contains("::1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun badNetmaskInput3() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.0.1/-1\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun validIPv4Input() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"192.168.0.1/32\"]"))
        MatcherAssert.assertThat(badlist.contains("192.168.0.1"), CoreMatchers.equalTo(false))
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class, JsonUtilsException::class, NetworkNodeException::class)
    fun validIPv6Input() {
        val mapper = ObjectMapper()
        val badlist = AnonymousIpWhitelist()
        badlist.init(mapper.readTree("[\"\"::1/128\"]"))
        MatcherAssert.assertThat(badlist.contains("::1"), CoreMatchers.equalTo(false))
    }

    /* NetworkNode takes forever to create Tree - commented out until it is needed
	@Test
	public void testAnonymousIpWhitelistPerformance65000() throws NetworkNodeException {
		AnonymousIpWhitelist whitelist = new AnonymousIpWhitelist();
		List<String> tempList = new ArrayList<>();
		// add a bunch of ips to the whitelist

		for (int i = 0; i < 255; i++) {
			for (int j = 0; j < 255; j++) {
				int a = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				int b = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				int c = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				int d = ThreadLocalRandom.current().nextInt(1, 254 + 1);
				tempList.add(String.format("%s.%s.%s.%s", a, b, c, d));
			}
		}

		long startTime = System.nanoTime();

		for (int i = 0; i < tempList.size(); i++) {
			whitelist.add(tempList.get(i) + "/32");
		}

		long durationTime = System.nanoTime() - startTime;

		System.out.println(String.format("Anonymous IP Whitelist creation took %s nanoseconds to create tree of %d subnets", Long.toString(durationTime),
				tempList.size()));

		int total = 1000;

		long start = System.nanoTime();

		for (int i = 0; i <= total; i++) {
			whitelist.contains("192.168.30.1");
		}

		long duration = System.nanoTime() - start;

		System.out.println(
				String.format("Anonymous IP Whitelist average lookup took %s nanoseconds for %d ips", Long.toString(duration / total), tempList.size()));
	}
	*/
    @Test
    @Throws(NetworkNodeException::class)
    fun testAddSubnets() {
        val whitelist = AnonymousIpWhitelist()
        whitelist.add("192.168.1.1/32")
        MatcherAssert.assertThat(whitelist.contains("192.168.1.1"), CoreMatchers.equalTo(true))
        whitelist.add("192.168.1.0/24")
        MatcherAssert.assertThat(whitelist.contains("192.168.1.255"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(whitelist.contains("192.168.1.167"), CoreMatchers.equalTo(true))
        whitelist.add("192.168.1.0/27")
        MatcherAssert.assertThat(whitelist.contains("192.168.1.255"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(whitelist.contains("192.168.1.167"), CoreMatchers.equalTo(true))
        whitelist.add("10.0.0.1/32")
        MatcherAssert.assertThat(whitelist.contains("10.0.0.1"), CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(whitelist.contains("10.0.0.2"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.168.2.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.168.2.255"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.167.1.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("192.169.1.1"), CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(whitelist.contains("10.0.0.0"), CoreMatchers.equalTo(false))
    }
}