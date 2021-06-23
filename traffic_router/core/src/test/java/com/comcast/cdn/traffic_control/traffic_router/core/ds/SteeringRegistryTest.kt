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
package com.comcast.cdn.traffic_control.traffic_router.core.ds

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test

class SteeringRegistryTest {
    @Test
    @Throws(Exception::class)
    fun itConsumesValidJson() {
        val json = "{ \"response\": [ " +
                "{ \"deliveryService\":\"steering-1\"," +
                "  \"targets\" : [" +
                "        {\"deliveryService\": \"steering-target-01\", \"weight\": 9876}," +
                "        {\"deliveryService\": \"steering-target-02\", \"weight\": 12345}" +
                "      ]," +
                "  \"filters\" : [" +
                "      { \"pattern\" : \".*/force-to-one/.*\", \"deliveryService\" : \"steering-target-01\" }," +
                "      { \"pattern\" : \".*/also-this/.*\", \"deliveryService\" : \"steering-target-01\" }" +
                "   ]" +
                "}, " +
                "{ \"deliveryService\":\"steering-2\"," +
                "  \"targets\" : [" +
                "        {\"deliveryService\": \"steering-target-3\", \"weight\": 1117}," +
                "        {\"deliveryService\": \"steering-target-02\", \"weight\": 556}" +
                "      ]" +
                "}" +
                "] }"
        val steeringRegistry = SteeringRegistry()
        steeringRegistry.update(json)
        MatcherAssert.assertThat(steeringRegistry.has("steering-1"), Matchers.equalTo(true))
        MatcherAssert.assertThat(steeringRegistry.has("steering-2"), Matchers.equalTo(true))
        val steeringTarget1 = SteeringTarget()
        steeringTarget1.deliveryService = "steering-target-01"
        steeringTarget1.weight = 9876
        val steeringTarget2 = SteeringTarget()
        steeringTarget2.deliveryService = "steering-target-02"
        steeringTarget2.weight = 12345
        MatcherAssert.assertThat(
            steeringRegistry["steering-1"].targets,
            Matchers.containsInAnyOrder(steeringTarget1, steeringTarget2)
        )
        MatcherAssert.assertThat(
            steeringRegistry["steering-2"].targets[1].deliveryService,
            Matchers.equalTo("steering-target-02")
        )
        MatcherAssert.assertThat(
            steeringRegistry["steering-1"].filters[0].pattern,
            Matchers.equalTo(".*/force-to-one/.*")
        )
        MatcherAssert.assertThat(
            steeringRegistry["steering-1"].filters[0].deliveryService,
            Matchers.equalTo("steering-target-01")
        )
        MatcherAssert.assertThat(steeringRegistry["steering-1"].filters[1].pattern, Matchers.equalTo(".*/also-this/.*"))
        MatcherAssert.assertThat(
            steeringRegistry["steering-1"].filters[1].deliveryService,
            Matchers.equalTo("steering-target-01")
        )
        MatcherAssert.assertThat(
            steeringRegistry["steering-1"].getBypassDestination("/stuff/force-to-one/more/stuff"),
            Matchers.equalTo("steering-target-01")
        )
        MatcherAssert.assertThat(
            steeringRegistry["steering-1"].getBypassDestination("/should/not/match"),
            Matchers.nullValue()
        )
        MatcherAssert.assertThat(steeringRegistry["steering-2"].filters.isEmpty(), Matchers.equalTo(true))
    }
}