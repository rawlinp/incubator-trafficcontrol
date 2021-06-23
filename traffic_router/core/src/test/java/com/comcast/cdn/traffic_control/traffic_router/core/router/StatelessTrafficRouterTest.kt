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
package com.comcast.cdn.traffic_control.traffic_router.core.router

import com.comcast.cdn.traffic_control.traffic_router.core.TestBase
import com.comcast.cdn.traffic_control.traffic_router.core.loc.GeolocationDatabaseUpdater
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkUpdater
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatelessTrafficRouterTest
import com.comcast.cdn.traffic_control.traffic_router.core.util.IntegrationTest
import org.apache.log4j.Logger
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.springframework.context.ApplicationContext
import java.nio.file.Files
import java.nio.file.Paths

@Category(IntegrationTest::class)
class StatelessTrafficRouterTest {
    private var trafficRouterManager: TrafficRouterManager? = null
    private var geolocationDatabaseUpdater: GeolocationDatabaseUpdater? = null
    private var networkUpdater: NetworkUpdater? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        trafficRouterManager = context!!.getBean("trafficRouterManager") as TrafficRouterManager
        geolocationDatabaseUpdater = context!!.getBean("geolocationDatabaseUpdater") as GeolocationDatabaseUpdater
        networkUpdater = context!!.getBean("networkUpdater") as NetworkUpdater
        while (!networkUpdater!!.isLoaded) {
            LOGGER.info("Waiting for a valid location database before proceeding")
            Thread.sleep(1000)
        }
        while (!geolocationDatabaseUpdater!!.isLoaded) {
            LOGGER.info("Waiting for a valid Maxmind database before proceeding")
            Thread.sleep(1000)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testRouteHTTPRequestTrack() {
        val req = HTTPRequest()
        req.clientIP = "10.0.0.1"
        req.path = "/QualityLevels(96000)/Fragments(audio_eng=20720000000)"
        req.queryString = ""
        req.hostname = "somehost.cdn.net"
        req.requestedUrl = "http://somehost.cdn.net/QualityLevels(96000)/Fragments(audio_eng=20720000000)"
        val track = StatTracker.getTrack()
        trafficRouterManager!!.trafficRouter.route(req, track)
    }

    companion object {
        private val LOGGER = Logger.getLogger(StatelessTrafficRouterTest::class.java)
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