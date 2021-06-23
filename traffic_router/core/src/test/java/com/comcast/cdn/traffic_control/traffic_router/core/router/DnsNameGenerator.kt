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

import com.fasterxml.jackson.databind.JsonNode

// Attempts to generate names like 'www.[foo].kabletown.com' to do dns queries against traffic router
// Tries to pull 'whole' words from the regex of cr-config
class DnsNameGenerator {
    @Throws(Exception::class)
    fun getNames(deliveryServicesConfig: JsonNode, cdnConfig: JsonNode): List<String> {
        val names: MutableList<String> = ArrayList()
        val domainName = cdnConfig["domain_name"].asText()
        for (matchsets in deliveryServicesConfig["matchsets"]) {
            for (matchset in matchsets) {
                if ("DNS" != matchset["protocol"].asText()) {
                    continue
                }
                for (matchlist in matchset["matchlist"]) {
                    val name = matchlist["regex"].asText()
                        .replace("\\.".toRegex(), "")
                        .replace("\\*".toRegex(), "")
                        .replace("\\\\".toRegex(), "")
                    names.add("edge.$name.$domainName")
                }
            }
        }
        return names
    }
}