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

import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.io.IOException

class AnonymousIpTest {
    private var trafficRouter: TrafficRouter? = null
    val configFile = File("src/test/resources/anonymous_ip.json")
    val configNoWhitelist = File("src/test/resources/anonymous_ip_no_whitelist.json")
    val mmdb = "src/test/resources/GeoIP2-Anonymous-IP.mmdb"
    var databaseFile = File(mmdb)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // ignore the test if there is no mmdb file
        val mmdbFile = File(mmdb)
        Assume.assumeTrue(mmdbFile.exists())
        AnonymousIp.parseConfigFile(configFile, false)
        assert(AnonymousIp.getCurrentConfig().iPv4Whitelist != null)
        assert(AnonymousIp.getCurrentConfig().iPv6Whitelist != null)

        // Set up a mock traffic router with real database
        val anonymousIpService = AnonymousIpDatabaseService()
        anonymousIpService.setDatabaseFile(databaseFile)
        anonymousIpService.reloadDatabase()
        assert(anonymousIpService.isInitialized)
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        Mockito.`when`(trafficRouter!!.anonymousIpDatabaseService).thenReturn(anonymousIpService)
    }

    @Test
    fun testConfigFileParsingIpv4() {
        val currentConfig = AnonymousIp.getCurrentConfig()
        MatcherAssert.assertThat(currentConfig, CoreMatchers.notNullValue())
        val whitelist = currentConfig.iPv4Whitelist
        MatcherAssert.assertThat(whitelist, CoreMatchers.notNullValue())
    }

    @Test
    fun testConfigFileParsingIpv6() {
        val currentConfig = AnonymousIp.getCurrentConfig()
        MatcherAssert.assertThat(currentConfig, CoreMatchers.notNullValue())
        val whitelist = currentConfig.iPv6Whitelist
        MatcherAssert.assertThat(whitelist, CoreMatchers.notNullValue())
    }

    @Test
    fun testIpInWhitelistIsAllowed() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "5.34.32.79"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    fun testFallsUnderManyPolicies() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2.38.158.142"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(true))
    }

    @Test
    fun testAllowNotCheckingPolicy() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2.36.248.52"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    @Throws(IOException::class)
    fun testEnforceAllowed() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "10.0.0.1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    @Throws(IOException::class)
    fun testEnforceAllowedIpInWhitelist() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "10.0.2.1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    fun testEnforceBlocked() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "223.26.48.248"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(true))
    }

    @Test
    fun testEnforceNotInWhitelistNotInDB() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "192.168.0.1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    /* IPv4 no whitelist */
    @Test
    fun testEnforceNoWhitelistAllowed() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "192.168.0.1"
        AnonymousIp.parseConfigFile(configNoWhitelist, false)
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    fun testEnforceNoWhitelistBlocked() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "223.26.48.248"
        AnonymousIp.parseConfigFile(configNoWhitelist, false)
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(true))
    }

    @Test
    fun testEnforceNoWhitelistNotEnforcePolicy() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2.36.248.52"
        AnonymousIp.parseConfigFile(configNoWhitelist, false)
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    /* IPv6 Testing */
    @Test
    fun testIpv6EnforceBlock() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2001:418:9807::1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(true))
    }

    @Test
    fun testIpv6EnforceNotBlock() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2001:418::1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    fun testIpv6EnforceNotBlockWhitelisted() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2001:550:90a:0:0:0:0:1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    fun testIpv6EnforceNotBlockOnWhitelist() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "::1"
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    /* IPv6 tests no whitelist */
    @Test
    fun testIpv6NoWhitelistEnforceBlock() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2001:418:9807::1"
        AnonymousIp.parseConfigFile(configNoWhitelist, false)
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(true))
    }

    @Test
    fun testIpv6NoWhitelistNoBlock() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "::1"
        AnonymousIp.parseConfigFile(configNoWhitelist, false)
        val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        MatcherAssert.assertThat(result, CoreMatchers.equalTo(false))
    }

    @Test
    fun testAnonymousIpPerformance() {
        val dsvcId = "dsID"
        val url = "http://ds1.example.com/live1"
        val ip = "2.36.248.52"
        val total: Long = 100000
        val start = System.nanoTime()
        for (i in 0..total) {
            val result = AnonymousIp.enforce(trafficRouter, dsvcId, url, ip)
        }
        val duration = System.nanoTime() - start
        println(
            String.format(
                "Anonymous IP blocking average took %s nanoseconds",
                java.lang.Long.toString(duration / total)
            )
        )
    }
}