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

import com.comcast.cdn.traffic_control.traffic_router.core.util.CidrAddress
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.core.IsNull
import org.junit.Test

class FederationMappingBuilderTest {
    @Test
    @Throws(Exception::class)
    fun itConsumesValidJSON() {
        val federationMappingBuilder = FederationMappingBuilder()
        val json = "{ " +
                "\"cname\" : \"cname1\", " +
                "\"ttl\" : \"86400\", " +
                "\"resolve4\" : [ \"192.168.56.78/24\", \"192.168.45.67/24\" ], " +
                "\"resolve6\" : [ \"fdfe:dcba:9876:5::/64\", \"fd12:3456:789a:1::/64\" ] " +
                "}"
        val federationMapping = federationMappingBuilder.fromJSON(json)
        MatcherAssert.assertThat(federationMapping, CoreMatchers.not(IsNull.nullValue()))
        MatcherAssert.assertThat(federationMapping.cname, Matchers.equalTo("cname1"))
        MatcherAssert.assertThat(federationMapping.ttl, Matchers.equalTo(86400))
        MatcherAssert.assertThat(
            federationMapping.resolve4,
            Matchers.containsInAnyOrder(
                CidrAddress.fromString("192.168.45.67/24"),
                CidrAddress.fromString("192.168.56.78/24")
            )
        )
        MatcherAssert.assertThat(
            federationMapping.resolve6,
            Matchers.containsInAnyOrder(
                CidrAddress.fromString("fd12:3456:789a:1::/64"),
                CidrAddress.fromString("fdfe:dcba:9876:5::/64")
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itConsumesJSONWithoutResolvers() {
        val federationMappingBuilder = FederationMappingBuilder()
        val json = "{ " +
                "\"cname\" : \"cname1\", " +
                "\"ttl\" : \"86400\" " +
                "}"
        val federationMapping = federationMappingBuilder.fromJSON(json)
        MatcherAssert.assertThat(federationMapping, CoreMatchers.not(IsNull.nullValue()))
        MatcherAssert.assertThat(federationMapping.cname, Matchers.equalTo("cname1"))
        MatcherAssert.assertThat(federationMapping.ttl, Matchers.equalTo(86400))
        MatcherAssert.assertThat(federationMapping.resolve4, CoreMatchers.not(IsNull.nullValue()))
        MatcherAssert.assertThat(federationMapping.resolve6, CoreMatchers.not(IsNull.nullValue()))
    }
}