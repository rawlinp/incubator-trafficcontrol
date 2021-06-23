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

import com.comcast.cdn.traffic_control.traffic_router.geolocation.GeolocationException
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class MaxmindGeoIP2Test {
    private var maxmindGeolocationService: MaxmindGeolocationService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        maxmindGeolocationService = MaxmindGeolocationService()
        val databaseFile = File(mmdb)
        maxmindGeolocationService!!.verifyDatabase(databaseFile)
        maxmindGeolocationService!!.setDatabaseFile(databaseFile)
    }

    @Test
    @Throws(GeolocationException::class)
    fun testSerialLookupPerformance() {
        val start = System.currentTimeMillis()
        val total = 100000
        for (i in 0..total) {
            maxmindGeolocationService!!.location("10.0.0.1")
        }
        val duration = System.currentTimeMillis() - start
        val tps = total.toDouble() / (duration.toDouble() / 1000)
        println("MaxMind2 lookup duration: " + duration + "ms, " + tps + " tps")
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        maxmindGeolocationService!!.destroy()
    }

    companion object {
        private const val mmdb = "src/test/db/GeoIP2-City.mmdb"
    }
}