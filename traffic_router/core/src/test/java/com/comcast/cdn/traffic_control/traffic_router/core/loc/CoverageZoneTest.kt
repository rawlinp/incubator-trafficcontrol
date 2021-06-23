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

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache.DeliveryServiceReference
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation.LocalizationMethod
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node.IPVersions
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox

@RunWith(PowerMockRunner::class)
@PrepareForTest(TrafficRouter::class)
class CoverageZoneTest {
    private var trafficRouter: TrafficRouter? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService.id).thenReturn("delivery-service-1")
        val deliveryServiceReference = DeliveryServiceReference("delivery-service-1", "some.example.com")
        val deliveryServices: MutableList<DeliveryServiceReference> = ArrayList()
        deliveryServices.add(deliveryServiceReference)
        val testLocation = Geolocation(40.0, -101.0)
        val farEastLocation = Geolocation(40.0, -101.5)
        val eastLocation = Geolocation(40.0, -100.0)
        val westLocation = Geolocation(40.0, -105.0)
        val farEastCache1 = Cache("far-east-cache-1", "hashid", 1)
        farEastCache1.setIsAvailable(true)
        val lms: MutableSet<LocalizationMethod> = HashSet()
        lms.add(LocalizationMethod.GEO)
        val farEastCacheGroup = CacheLocation("far-east-cache-group", farEastLocation, lms)
        farEastCacheGroup.addCache(farEastCache1)
        farEastCache1.deliveryServices = deliveryServices
        val eastCache1 = Cache("east-cache-1", "hashid", 1)
        eastCache1.setIsAvailable(true)
        val eastCacheGroup = CacheLocation("east-cache-group", eastLocation)
        eastCacheGroup.addCache(eastCache1)
        val westCache1 = Cache("west-cache-1", "hashid", 1)
        westCache1.setIsAvailable(true)
        westCache1.deliveryServices = deliveryServices
        val westCacheGroup = CacheLocation("west-cache-group", westLocation)
        westCacheGroup.addCache(westCache1)
        val cacheGroups: MutableList<CacheLocation> = ArrayList()
        cacheGroups.add(farEastCacheGroup)
        cacheGroups.add(eastCacheGroup)
        cacheGroups.add(westCacheGroup)
        val eastNetworkNode = NetworkNode("12.23.34.0/24", "east-cache-group", testLocation)
        val cacheRegister = Mockito.mock(CacheRegister::class.java)
        Mockito.`when`(cacheRegister.getCacheLocationById("east-cache-group")).thenReturn(eastCacheGroup)
        Mockito.`when`(cacheRegister.filterAvailableCacheLocations("delivery-service-1")).thenReturn(cacheGroups)
        Mockito.`when`(cacheRegister.getDeliveryService("delivery-service-1")).thenReturn(deliveryService)
        val trafficRouter = PowerMockito.mock(TrafficRouter::class.java)
        Whitebox.setInternalState(trafficRouter, "cacheRegister", cacheRegister)
        Mockito.`when`(
            trafficRouter.getCoverageZoneCacheLocation(
                "12.23.34.45",
                "delivery-service-1",
                IPVersions.IPV4ONLY
            )
        ).thenCallRealMethod()
        Mockito.`when`(
            trafficRouter.getCoverageZoneCacheLocation(
                "12.23.34.45",
                "delivery-service-1",
                false,
                null,
                IPVersions.IPV4ONLY
            )
        ).thenCallRealMethod()
        Mockito.`when`(trafficRouter.getCacheRegister()).thenReturn(cacheRegister)
        Mockito.`when`(
            trafficRouter.orderLocations(
                Matchers.anyListOf(
                    CacheLocation::class.java
                ), Matchers.any(Geolocation::class.java)
            )
        ).thenCallRealMethod()
        Mockito.`when`(
            trafficRouter.getSupportingCaches(
                Matchers.anyListOf(
                    Cache::class.java
                ), Matchers.eq(deliveryService), Matchers.any(
                    IPVersions::class.java
                )
            )
        ).thenCallRealMethod()
        Mockito.`when`(
            trafficRouter.filterEnabledLocations(
                Matchers.anyListOf(
                    CacheLocation::class.java
                ), Matchers.any(LocalizationMethod::class.java)
            )
        ).thenCallRealMethod()
        PowerMockito.`when`<Any>(trafficRouter, "getNetworkNode", "12.23.34.45").thenReturn(eastNetworkNode)
        PowerMockito.`when`<Any>(
            trafficRouter, "getClosestCacheLocation", Matchers.anyListOf(
                CacheLocation::class.java
            ), Matchers.any(CacheLocation::class.java), Matchers.any(
                DeliveryService::class.java
            ), Matchers.any(IPVersions::class.java)
        ).thenCallRealMethod()
        this.trafficRouter = trafficRouter
    }

    @Test
    @Throws(Exception::class)
    fun trafficRouterReturnsNearestCacheGroupForDeliveryService() {
        val cacheLocation =
            trafficRouter!!.getCoverageZoneCacheLocation("12.23.34.45", "delivery-service-1", IPVersions.IPV4ONLY)
        MatcherAssert.assertThat(cacheLocation.id, org.hamcrest.Matchers.equalTo("west-cache-group"))
        // NOTE: far-east-cache-group is actually closer to the client but isn't enabled for CZ-localization and must be filtered out
    }
}