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

import com.comcast.cdn.traffic_control.traffic_router.core.config.ParseException
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryServiceMatcher
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache.DeliveryServiceReference
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtils
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtilsException
import com.fasterxml.jackson.databind.JsonNode
import java.net.UnknownHostException
import java.util.*

object CacheRegisterBuilder {
    @Throws(JsonUtilsException::class, ParseException::class)
    fun parseCacheConfig(contentServers: JsonNode, cacheRegister: CacheRegister) {
        val map: MutableMap<String, Cache> = HashMap()
        val statMap: MutableMap<String, MutableList<String>> = HashMap()
        val contentServersIter = contentServers.fieldNames()
        while (contentServersIter.hasNext()) {
            val node = contentServersIter.next()
            val jo = JsonUtils.getJsonNode(contentServers, node)
            val loc = cacheRegister.getCacheLocation(JsonUtils.getString(jo, "locationId"))
            if (loc != null) {
                var hashId = node
                if (jo.has("hashId")) {
                    hashId = jo["hashId"].asText()
                }
                val hashCount = JsonUtils.optInt(jo, "hashCount")
                val cache = Cache(node, hashId, hashCount)
                cache.fqdn = JsonUtils.getString(jo, "fqdn")
                cache.port = JsonUtils.getInt(jo, "port")
                val ip = JsonUtils.getString(jo, "ip")
                val ip6 = JsonUtils.optString(jo, "ip6")
                try {
                    cache.setIpAddress(ip, ip6, 0)
                } catch (e: UnknownHostException) {
                    println("$e: $ip")
                }
                if (jo.has("deliveryServices")) {
                    val references: MutableList<DeliveryServiceReference> = ArrayList()
                    val dsJos = jo["deliveryServices"]
                    val dsIter = dsJos.fieldNames()
                    while (dsIter.hasNext()) {
                        val ds = dsIter.next()
                        val dso = dsJos[ds]
                        var dsNames = statMap[ds]
                        if (dsNames == null) {
                            dsNames = ArrayList()
                        }
                        if (dso.isArray) {
                            var i = 0
                            for (fqdn in dso) {
                                val name = fqdn.asText().toLowerCase()
                                if (i == 0) {
                                    references.add(DeliveryServiceReference(ds, name))
                                }
                                val tld = JsonUtils.optString(cacheRegister.config, "domain_name").toLowerCase()
                                if (name.contains(tld)) {
                                    val reName = name.replace("^.*?\\.".toRegex(), "")
                                    if (!dsNames.contains(reName)) {
                                        dsNames.add(reName)
                                    }
                                } else {
                                    if (!dsNames.contains(name)) {
                                        dsNames.add(name)
                                    }
                                }
                                i++
                            }
                        } else {
                            references.add(DeliveryServiceReference(ds, dso.toString()))
                            if (!dsNames.contains(dso.toString())) {
                                dsNames.add(dso.toString())
                            }
                        }
                        statMap[ds] = dsNames
                    }
                    cache.deliveryServices = references
                }
                loc.addCache(cache)
                map[cache.id] = cache
            }
        }
        cacheRegister.cacheMap = map
    }

    @Throws(JsonUtilsException::class)
    fun parseDeliveryServiceConfig(deliveryServices: JsonNode, cacheRegister: CacheRegister) {
        val deliveryServiceMatchers = TreeSet<DeliveryServiceMatcher>()
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        val keyIter = deliveryServices.fieldNames()
        while (keyIter.hasNext()) {
            val dsId = keyIter.next()
            val dsJo = JsonUtils.getJsonNode(deliveryServices, dsId)
            val matchsets = JsonUtils.getJsonNode(dsJo, "machsets")
            val ds = DeliveryService(dsId, dsJo)
            val isDns = false
            dsMap[dsId] = ds
            for (matchset in matchsets) {
                val protocol = JsonUtils.getString(matchset, "protocol")
                val m = DeliveryServiceMatcher(ds)
                deliveryServiceMatchers.add(m)
                for (matchlist in matchset["matchlist"]) {
                    val type = DeliveryServiceMatcher.Type.valueOf(JsonUtils.getString(matchlist, "match-type"))
                    val target = JsonUtils.optString(matchlist, "target")
                    m.addMatch(type, JsonUtils.getString(matchlist, "regex"), target)
                }
            }
            ds.isDns = isDns
        }
        cacheRegister.setDeliveryServiceMap(dsMap)
        cacheRegister.setDeliveryServiceMatchers(deliveryServiceMatchers)
    }
}