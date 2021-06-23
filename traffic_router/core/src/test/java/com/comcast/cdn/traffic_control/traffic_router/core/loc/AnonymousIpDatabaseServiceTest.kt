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

import com.maxmind.geoip2.exception.GeoIp2Exception
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

class AnonymousIpDatabaseServiceTest {
    private var anonymousIpService: AnonymousIpDatabaseService? = null

    @Before
    @Throws(Exception::class)
    fun setup() {
        // ignore the test if there is no mmdb file
        val mmdbFile = File(mmdb)
        Assume.assumeTrue(mmdbFile.exists())
        anonymousIpService = AnonymousIpDatabaseService()
        val databaseFile = File(mmdb)
        anonymousIpService!!.setDatabaseFile(databaseFile)
        anonymousIpService!!.reloadDatabase()
        assert(anonymousIpService!!.isInitialized)
    }

    @Test
    @Throws(Exception::class)
    fun testIpInDatabase() {
        MatcherAssert.assertThat(
            anonymousIpService!!.lookupIp(InetAddress.getByName("223.26.48.248")),
            CoreMatchers.notNullValue()
        )
        MatcherAssert.assertThat(
            anonymousIpService!!.lookupIp(InetAddress.getByName("223.26.48.248")),
            CoreMatchers.notNullValue()
        )
        MatcherAssert.assertThat(
            anonymousIpService!!.lookupIp(InetAddress.getByName("1.1.205.152")),
            CoreMatchers.notNullValue()
        )
        MatcherAssert.assertThat(
            anonymousIpService!!.lookupIp(InetAddress.getByName("18.85.22.204")),
            CoreMatchers.notNullValue()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIpNotInDatabase() {
        MatcherAssert.assertThat(
            anonymousIpService!!.lookupIp(InetAddress.getByName("192.168.0.1")),
            CoreMatchers.equalTo(null)
        )
    }

    @Test
    @Throws(UnknownHostException::class, IOException::class, GeoIp2Exception::class)
    fun testDatabaseNotLoaded() {
        val anonymousIpService = AnonymousIpDatabaseService()
        MatcherAssert.assertThat(anonymousIpService.isInitialized, CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(
            anonymousIpService.lookupIp(InetAddress.getByName("223.26.48.248")),
            CoreMatchers.equalTo(null)
        )
        MatcherAssert.assertThat(
            anonymousIpService.lookupIp(InetAddress.getByName("192.168.0.1")),
            CoreMatchers.equalTo(null)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testLookupTime() {
        val ipAddress = InetAddress.getByName("223.26.48.248")
        val start = System.nanoTime()
        val total: Long = 100000
        for (i in 0..total) {
            anonymousIpService!!.lookupIp(ipAddress)
        }
        val duration = System.nanoTime() - start
        println(
            String.format(
                "Anonymous IP database average lookup: %s nanoseconds",
                java.lang.Long.toString(duration / total)
            )
        )
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        try {
            anonymousIpService!!.finalize()
        } catch (e: Throwable) {
        }
    }

    companion object {
        private const val mmdb = "src/test/resources/GeoIP2-Anonymous-IP.mmdb"
    }
}