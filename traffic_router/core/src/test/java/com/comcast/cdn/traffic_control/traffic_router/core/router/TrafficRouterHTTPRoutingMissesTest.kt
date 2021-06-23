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

import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.reflect.Whitebox

class TrafficRouterHTTPRoutingMissesTest {
    private var request: HTTPRequest? = null
    private var trafficRouter: TrafficRouter = Mockito.mock(TrafficRouter::class.java)
    private var track: StatTracker.Track = Mockito.mock(StatTracker.Track::class.java)
    private var cacheRegister: CacheRegister? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        request = HTTPRequest()
        request!!.clientIP = "192.168.34.56"
        cacheRegister = Mockito.mock(CacheRegister::class.java)
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        track = PowerMockito.spy(StatTracker.getTrack())
        Whitebox.setInternalState(trafficRouter, "cacheRegister", cacheRegister)
        PowerMockito.doCallRealMethod().`when`(trafficRouter).route(request, track)
        PowerMockito.doCallRealMethod().`when`(trafficRouter).singleRoute(request, track)
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenNoDeliveryService() {
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.DS_MISS)
        Mockito.verify(track).setResultDetails(ResultDetails.DS_NOT_FOUND)
    }
}