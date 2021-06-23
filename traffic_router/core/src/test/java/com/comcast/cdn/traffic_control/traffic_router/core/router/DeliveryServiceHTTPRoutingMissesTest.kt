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
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.powermock.reflect.Whitebox

class DeliveryServiceHTTPRoutingMissesTest {
    private var deliveryService: DeliveryService? = null
    private var httpRequest: HTTPRequest? = null
    private var track: StatTracker.Track? = null
    private var bypassDestination: JsonNode? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        val mapper = ObjectMapper()
        val unusedByTest = Mockito.mock(
            JsonNode::class.java
        )
        val ttls = Mockito.mock(
            JsonNode::class.java
        )
        Mockito.`when`(unusedByTest["ttls"]).thenReturn(ttls)
        Mockito.`when`(unusedByTest.has("routingName")).thenReturn(true)
        Mockito.`when`(unusedByTest["routingName"]).thenReturn(mapper.readTree("\"edge\""))
        Mockito.`when`(unusedByTest.has("coverageZoneOnly")).thenReturn(true)
        Mockito.`when`(unusedByTest["coverageZoneOnly"]).thenReturn(mapper.readTree("true"))
        Mockito.`when`(unusedByTest.has("deepCachingType")).thenReturn(true)
        Mockito.`when`(unusedByTest["deepCachingType"]).thenReturn(mapper.readTree("\"NEVER\""))
        deliveryService = DeliveryService("ignoredbytest", unusedByTest)
        httpRequest = Mockito.mock(HTTPRequest::class.java)
        track = StatTracker.getTrack()
        bypassDestination = Mockito.mock(JsonNode::class.java)
        Whitebox.setInternalState(deliveryService, "bypassDestination", bypassDestination)
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenNoBypass() {
        val nullBypassDestination: JsonNode? = null
        Whitebox.setInternalState(deliveryService, "bypassDestination", nullBypassDestination)
        deliveryService!!.getFailureHttpResponse(httpRequest, track)
        MatcherAssert.assertThat(track!!.getResultDetails(), Matchers.equalTo(ResultDetails.DS_NO_BYPASS))
        MatcherAssert.assertThat(track!!.getResult(), Matchers.equalTo(ResultType.MISS))
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenNoHTTPBypass() {
        Mockito.`when`(bypassDestination!!["HTTP"]).thenReturn(null)
        deliveryService!!.getFailureHttpResponse(httpRequest, track)
        MatcherAssert.assertThat(track!!.getResultDetails(), Matchers.equalTo(ResultDetails.DS_NO_BYPASS))
        MatcherAssert.assertThat(track!!.getResult(), Matchers.equalTo(ResultType.MISS))
    }

    @Test
    @Throws(Exception::class)
    fun itSetsDetailsWhenNoFQDNBypass() {
        val mapper = ObjectMapper()
        var httpJsonObject: JsonNode = mapper.createObjectNode()
        httpJsonObject = Mockito.spy(httpJsonObject)
        Mockito.doReturn(null).`when`(httpJsonObject)["fqdn"]
        Mockito.`when`(bypassDestination!!["HTTP"]).thenReturn(httpJsonObject)
        deliveryService!!.getFailureHttpResponse(httpRequest, track)
        Mockito.verify(httpJsonObject)["fqdn"]
        MatcherAssert.assertThat(track!!.getResultDetails(), Matchers.equalTo(ResultDetails.DS_NO_BYPASS))
        MatcherAssert.assertThat(track!!.getResult(), Matchers.equalTo(ResultType.MISS))
    }
}