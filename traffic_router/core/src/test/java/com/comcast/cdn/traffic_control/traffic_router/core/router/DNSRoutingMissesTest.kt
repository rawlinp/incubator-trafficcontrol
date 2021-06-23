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
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node.IPVersions
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationRegistry
import com.comcast.cdn.traffic_control.traffic_router.core.request.DNSRequest
import com.comcast.cdn.traffic_control.traffic_router.core.request.Request
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import com.comcast.cdn.traffic_control.traffic_router.core.util.CidrAddress
import com.fasterxml.jackson.databind.JsonNode
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import org.xbill.DNS.Name
import org.xbill.DNS.Type

@RunWith(PowerMockRunner::class)
@PrepareForTest(DeliveryService::class, TrafficRouter::class)
class DNSRoutingMissesTest {
    private var request: DNSRequest? = null
    private var trafficRouter: TrafficRouter = Mockito.mock(TrafficRouter::class.java)
    private var track: StatTracker.Track = Mockito.mock(StatTracker.Track::class.java)

    @Before
    @Throws(Exception::class)
    fun before() {
        val name = Name.fromString("edge.foo-img.kabletown.com")
        request = DNSRequest("foo-img.kabletown.com", name, Type.A)
        request!!.clientIP = "192.168.34.56"
        request!!.hostname = name.relativize(Name.root).toString()
        val federationRegistry = Mockito.mock(FederationRegistry::class.java)
        Mockito.`when`(
            federationRegistry.findInetRecords(
                Matchers.anyString(), Matchers.any(
                    CidrAddress::class.java
                )
            )
        ).thenReturn(null)
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        Mockito.`when`(trafficRouter.getCacheRegister()).thenReturn(
            Mockito.mock(
                CacheRegister::class.java
            )
        )
        Whitebox.setInternalState(trafficRouter, "federationRegistry", federationRegistry)
        Mockito.`when`(
            trafficRouter.selectCachesByGeo(
                Matchers.anyString(), Matchers.any(
                    DeliveryService::class.java
                ), Matchers.any(CacheLocation::class.java), Matchers.any(
                    StatTracker.Track::class.java
                ), Matchers.any(IPVersions::class.java)
            )
        ).thenCallRealMethod()
        track = PowerMockito.spy(StatTracker.getTrack())
        PowerMockito.doCallRealMethod().`when`(trafficRouter).route(request, track)
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenNoDeliveryService() {
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.MISS)
        Mockito.verify(track).setResultDetails(ResultDetails.LOCALIZED_DNS)
    }

    // When the delivery service is unavailable ...
    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenNoBypass() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService.isAvailable).thenReturn(false)
        Mockito.`when`(deliveryService.getFailureDnsResponse(request, track)).thenCallRealMethod()
        Mockito.`when`(deliveryService.routingName).thenReturn("edge")
        Mockito.`when`(deliveryService.isDns).thenReturn(true)
        Mockito.doReturn(deliveryService).`when`(trafficRouter).selectDeliveryService(request)
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.MISS)
        Mockito.verify(track).setResultDetails(ResultDetails.DS_NO_BYPASS)
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenBypassDestination() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(deliveryService.isAvailable).thenReturn(false)
        Mockito.`when`(deliveryService.getFailureDnsResponse(request, track)).thenCallRealMethod()
        Mockito.`when`(deliveryService.routingName).thenReturn("edge")
        Mockito.`when`(deliveryService.isDns).thenReturn(true)
        Mockito.doReturn(deliveryService).`when`(trafficRouter).selectDeliveryService(request)
        val bypassDestination = Mockito.mock(
            JsonNode::class.java
        )
        Mockito.`when`(bypassDestination["DNS"]).thenReturn(null)
        Whitebox.setInternalState(deliveryService, "bypassDestination", bypassDestination)
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.DS_REDIRECT)
        Mockito.verify(track).setResultDetails(ResultDetails.DS_BYPASS)
    }

    // The Delivery Service is available but we don't find the cache in the coverage zone map
    // - and DS doesn't support other lookups
    @Test
    @Throws(Exception::class)
    fun itSetsDetailsAboutMissesWhenOnlyCoverageZoneSupported() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.doReturn(true).`when`(deliveryService).isAvailable
        Mockito.`when`(deliveryService.routingName).thenReturn("edge")
        Mockito.`when`(deliveryService.isDns).thenReturn(true)
        Mockito.`when`(deliveryService.isCoverageZoneOnly).thenReturn(true)
        Mockito.doReturn(deliveryService).`when`(trafficRouter).selectDeliveryService(
            Matchers.any(
                Request::class.java
            )
        )
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.MISS)
        Mockito.verify(track).setResultDetails(ResultDetails.DS_CZ_ONLY)
    }

    // 1. We got an unsupported cache location from the coverage zone map
    // 2. we looked up the client location from maxmind
    // 3. delivery service says the client location is unsupported
    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenClientGeolocationNotSupported() {
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.doReturn(true).`when`(deliveryService).isAvailable
        Mockito.`when`(deliveryService.routingName).thenReturn("edge")
        Mockito.`when`(deliveryService.isDns).thenReturn(true)
        Mockito.`when`(deliveryService.isCoverageZoneOnly).thenReturn(false)
        Mockito.doReturn(deliveryService).`when`(trafficRouter).selectDeliveryService(request)
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.MISS)
        Mockito.verify(track).setResultDetails(ResultDetails.DS_CLIENT_GEO_UNSUPPORTED)
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenCacheNotFoundByGeolocation() {
        PowerMockito.doCallRealMethod().`when`(trafficRouter).selectCachesByGeo(
            Matchers.anyString(), Matchers.any(
                DeliveryService::class.java
            ), Matchers.any(CacheLocation::class.java), Matchers.any(
                StatTracker.Track::class.java
            ), Matchers.any(IPVersions::class.java)
        )
        val cacheLocation = Mockito.mock(CacheLocation::class.java)
        val cacheRegister = Mockito.mock(CacheRegister::class.java)
        val deliveryService = Mockito.mock(DeliveryService::class.java)
        Mockito.doReturn(true).`when`(deliveryService).isAvailable
        Mockito.`when`(deliveryService.isLocationAvailable(cacheLocation)).thenReturn(false)
        Mockito.`when`(deliveryService.isCoverageZoneOnly).thenReturn(false)
        Mockito.`when`(deliveryService.routingName).thenReturn("edge")
        Mockito.`when`(deliveryService.isDns).thenReturn(true)
        Mockito.doReturn(deliveryService).`when`(trafficRouter).selectDeliveryService(request)
        Mockito.doReturn(cacheLocation).`when`(trafficRouter)
            .getCoverageZoneCacheLocation("192.168.34.56", deliveryService, IPVersions.IPV4ONLY)
        Mockito.doReturn(cacheRegister).`when`(trafficRouter).cacheRegister
        trafficRouter.route(request, track)
        Mockito.verify(track).setResult(ResultType.MISS)
        Mockito.verify(track).setResultDetails(ResultDetails.DS_CLIENT_GEO_UNSUPPORTED)
    }
}