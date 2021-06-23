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

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringResult
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringTarget
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(Collections::class)
class GeoSortSteeringResultsTest {
    private var trafficRouter: TrafficRouter = Mockito.mock(TrafficRouter::class.java)
    private var steeringResults: MutableList<SteeringResult>? = null
    private var clientLocation: Geolocation? = null
    private var deliveryService: DeliveryService? = null

    @Before
    fun before() {
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        steeringResults = ArrayList<SteeringResult>() as MutableList<SteeringResult>
        clientLocation = Geolocation(47.0, -122.0)
        deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.doCallRealMethod().`when`(trafficRouter).geoSortSteeringResults(
            Matchers.anyListOf(
                SteeringResult::class.java
            ), Matchers.anyString(), Matchers.any(
                DeliveryService::class.java
            )
        )
        Mockito.`when`(
            trafficRouter.getClientLocationByCoverageZoneOrGeo(
                Matchers.anyString(), Matchers.any(
                    DeliveryService::class.java
                )
            )
        ).thenReturn(clientLocation)
    }

    @Test
    fun testNullClientIP() {
        trafficRouter.geoSortSteeringResults(steeringResults!!, null, deliveryService)
        Mockito.verify(trafficRouter, Mockito.never()).getClientLocationByCoverageZoneOrGeo(null, deliveryService)
    }

    @Test
    fun testEmptyClientIP() {
        trafficRouter.geoSortSteeringResults(steeringResults, "", deliveryService)
        Mockito.verify(trafficRouter, Mockito.never()).getClientLocationByCoverageZoneOrGeo("", deliveryService)
    }

    @Test
    fun testNoSteeringTargetsHaveGeolocations() {
        steeringResults!!.add(SteeringResult(SteeringTarget(), deliveryService))
        trafficRouter.geoSortSteeringResults(steeringResults, "::1", deliveryService)
        Mockito.verify(trafficRouter, Mockito.never()).getClientLocationByCoverageZoneOrGeo("::1", deliveryService)
    }

    @Test
    fun testClientGeolocationIsNull() {
        val steeringTarget = SteeringTarget()
        steeringTarget.geolocation = clientLocation
        steeringResults!!.add(SteeringResult(steeringTarget, deliveryService))
        clientLocation = null
        PowerMockito.mockStatic(Collections::class.java)
        trafficRouter.geoSortSteeringResults(steeringResults, "::1", deliveryService)
        PowerMockito.verifyStatic(Mockito.never())
    }

    @Test
    fun testGeoSortingMixedWithNonGeoTargets() {
        val cache = Cache("fake-id", "fake-hash-id", 1, clientLocation)
        var target: SteeringTarget
        target = SteeringTarget()
        target.order = -1
        val resultNoGeoNegativeOrder = SteeringResult(target, deliveryService)
        resultNoGeoNegativeOrder.cache = cache
        steeringResults!!.add(resultNoGeoNegativeOrder)
        target = SteeringTarget()
        target.order = 1
        val resultNoGeoPositiveOrder = SteeringResult(target, deliveryService)
        resultNoGeoPositiveOrder.cache = cache
        steeringResults!!.add(resultNoGeoPositiveOrder)
        target = SteeringTarget()
        target.order = 0
        val resultNoGeoZeroOrder = SteeringResult(target, deliveryService)
        resultNoGeoZeroOrder.cache = cache
        steeringResults!!.add(resultNoGeoZeroOrder)
        target = SteeringTarget()
        target.geolocation = clientLocation
        target.order = 0
        val resultGeo = SteeringResult(target, deliveryService)
        resultGeo.cache = cache
        steeringResults!!.add(resultGeo)
        trafficRouter.geoSortSteeringResults(steeringResults, "::1", deliveryService)
        Assert.assertEquals(resultNoGeoNegativeOrder, steeringResults!![0])
        Assert.assertEquals(resultGeo, steeringResults!![1])
        Assert.assertEquals(resultNoGeoZeroOrder, steeringResults!![2])
        Assert.assertEquals(resultNoGeoPositiveOrder, steeringResults!![3])
    }
}