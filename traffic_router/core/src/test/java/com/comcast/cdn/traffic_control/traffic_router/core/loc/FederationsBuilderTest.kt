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

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.core.IsNull
import org.junit.Test

class FederationsBuilderTest {
    @Test
    @Throws(Exception::class)
    fun itConsumesValidJSON() {
        val federationsBuilder = FederationsBuilder()
        val json = "{ \"response\": [ " +
                "{ " +
                "\"deliveryService\" : \"kable-town-01\", " +
                "\"mappings\" : [ " +
                "{ \"cname\" : \"cname1\", " +
                "\"ttl\" : \"86400\", " +
                "\"resolve4\" : [ \"192.168.56.78/24\", \"192.168.45.67/24\" ], " +
                "\"resolve6\" : [ \"fdfe:dcba:9876:5::/64\", \"fd12:3456:789a:1::/64\" ] " +
                "}, " +
                "{ \"cname\" : \"cname2\", \"ttl\" : \"86400\" } " +
                "] " +
                "}, " +
                "{ " +
                "\"deliveryService\" : \"kable-town-02\", " +
                "\"mappings\" : [ { \"cname\" : \"cname4\" , \"ttl\" : \"86400\" } ]" +
                "} " +
                "] }"
        val federations = federationsBuilder.fromJSON(json)
        MatcherAssert.assertThat(federations.size, Matchers.equalTo(2))
        MatcherAssert.assertThat(federations[0].deliveryService, Matchers.equalTo("kable-town-01"))
        MatcherAssert.assertThat(federations[0].federationMappings, CoreMatchers.not(IsNull.nullValue()))
    }
}