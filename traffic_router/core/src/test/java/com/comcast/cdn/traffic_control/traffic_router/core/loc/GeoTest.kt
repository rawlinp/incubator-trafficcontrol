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

import com.comcast.cdn.traffic_control.traffic_router.core.TestBase
import com.comcast.cdn.traffic_control.traffic_router.core.loc.GeoTest
import com.comcast.cdn.traffic_control.traffic_router.core.util.IntegrationTest
import org.apache.log4j.Logger
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.springframework.context.ApplicationContext
import java.nio.file.Files
import java.nio.file.Paths

@Category(IntegrationTest::class)
class GeoTest {
    private var geolocationDatabaseUpdater: GeolocationDatabaseUpdater? = null
    private var maxmindGeolocationService: MaxmindGeolocationService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        geolocationDatabaseUpdater = context!!.getBean("geolocationDatabaseUpdater") as GeolocationDatabaseUpdater
        maxmindGeolocationService = context!!.getBean("maxmindGeolocationService") as MaxmindGeolocationService
        geolocationDatabaseUpdater!!.loadDatabase()
        while (!geolocationDatabaseUpdater!!.isLoaded) {
            LOGGER.info("Waiting for a valid Maxmind database before proceeding")
            Thread.sleep(1000)
        }
    }

    @Test
    fun testIps() {
        try {
            val testips = arrayOf(arrayOf("40.40.40.40", "cache-group-1"))
            for (i in testips.indices) {
                val location = maxmindGeolocationService!!.location(testips[i][0])
                Assert.assertNotNull(location)
                val loc = location.toString()
                LOGGER.info(String.format("result for ip=%s: %s\n", testips[i], loc))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(GeoTest::class.java)
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