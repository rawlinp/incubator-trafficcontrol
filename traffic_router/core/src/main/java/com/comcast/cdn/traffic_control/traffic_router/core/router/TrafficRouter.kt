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

import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.Random
import java.util.regex.Pattern
import java.util.stream.Collectors

import com.comcast.cdn.traffic_control.traffic_router.configuration.ConfigurationListener
import com.comcast.cdn.traffic_control.traffic_router.core.dns.DNSAccessRecord
import com.comcast.cdn.traffic_control.traffic_router.core.dns.ZoneManager
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringGeolocationComparator
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringRegistry
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringResult
import com.comcast.cdn.traffic_control.traffic_router.core.ds.SteeringTarget
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation.LocalizationMethod
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.edge.InetRecord
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Location
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node.IPVersions
import com.comcast.cdn.traffic_control.traffic_router.core.edge.TrafficRouterLocation
import com.comcast.cdn.traffic_control.traffic_router.core.hash.ConsistentHasher
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AnonymousIp
import com.comcast.cdn.traffic_control.traffic_router.core.loc.AnonymousIpDatabaseService
import com.comcast.cdn.traffic_control.traffic_router.core.loc.FederationRegistry
import com.comcast.cdn.traffic_control.traffic_router.core.loc.MaxmindGeolocationService
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNode
import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNodeException
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeo
import com.comcast.cdn.traffic_control.traffic_router.core.request.DNSRequest
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.request.Request
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultDetails
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.ResultType
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker.Track.RouteType
import com.comcast.cdn.traffic_control.traffic_router.core.util.CidrAddress
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtils
import com.comcast.cdn.traffic_control.traffic_router.core.util.TrafficOpsUtils
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import com.comcast.cdn.traffic_control.traffic_router.geolocation.GeolocationException
import com.comcast.cdn.traffic_control.traffic_router.geolocation.GeolocationService

import com.fasterxml.jackson.databind.JsonNode
import org.apache.log4j.Logger
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.xbill.DNS.Name
import org.xbill.DNS.Type
import org.xbill.DNS.Zone

/**
 * TrafficRouter is the main router class that handles Traffic Router logic.
 */
class TrafficRouter(cr: CacheRegister,
                    geolocationService: GeolocationService,
                    geolocationService6: GeolocationService,
                    anonymousIpService: AnonymousIpDatabaseService,
                    statTracker: StatTracker,
                    trafficOpsUtils: TrafficOpsUtils,
                    federationRegistry: FederationRegistry,
                    trafficRouterManager: TrafficRouterManager) {
    val cacheRegister: CacheRegister
    val zoneManager: ZoneManager
    val geolocationService: GeolocationService
    private val geolocationService6: GeolocationService
    val anonymousIpDatabaseService: AnonymousIpDatabaseService
    private val federationRegistry: FederationRegistry
    val isConsistentDNSRouting: Boolean
    private val isClientSteeringDiversityEnabled: Boolean
    val isDnssecZoneDiffingEnabled: Boolean
    val isEdgeDNSRouting: Boolean
    val isEdgeHTTPRouting: Boolean
    private var edgeNSttl: Long = 0 // 1 hour default
    private val edgeDNSRoutingLimit: Int
    private val edgeHTTPRoutingLimit: Int
    private val random = Random(System.nanoTime())
    var requestHeaders: Set<String> = HashSet()
    private var applicationContext: ApplicationContext? = null
    private val consistentHasher = ConsistentHasher()
    private var steeringRegistry: SteeringRegistry? = null
    private val defaultGeolocationsOverride: MutableMap<String, Geolocation> = HashMap()
    // TODO: consolidate this public property with the above private val. Currently, a unit test depends on mocking this public property
    val defaultGeoLocationsOverride: Map<String, Geolocation>
        get() = defaultGeolocationsOverride

    init {
        cacheRegister = cr
        this.geolocationService = geolocationService
        this.geolocationService6 = geolocationService6
        anonymousIpDatabaseService = anonymousIpService
        this.federationRegistry = federationRegistry
        isClientSteeringDiversityEnabled = JsonUtils.optBoolean(cr.config, CLIENT_STEERING_DIVERSITY)
        isDnssecZoneDiffingEnabled = JsonUtils.optBoolean(cr.config, DNSSEC_ENABLED) && JsonUtils.optBoolean(cr.config, DNSSEC_ZONE_DIFFING)
        isConsistentDNSRouting = JsonUtils.optBoolean(cr.config, "consistent.dns.routing") // previous/default behavior
        isEdgeDNSRouting = JsonUtils.optBoolean(cr.config, "edge.dns.routing") && cr.hasEdgeTrafficRouters()
        isEdgeHTTPRouting = JsonUtils.optBoolean(cr.config, "edge.http.routing") && cr.hasEdgeTrafficRouters()
        if (cr.config != null) {
            // maxmindDefaultOverride: {countryCode: , lat: , long: }
            val geolocations = cr.config["maxmindDefaultOverride"]
            if (geolocations != null) {
                for (geolocation in geolocations) {
                    val countryCode = JsonUtils.optString(geolocation, "countryCode")
                    val lat = JsonUtils.optDouble(geolocation, "lat")
                    val longitude = JsonUtils.optDouble(geolocation, "long")
                    defaultGeolocationsOverride[countryCode] = Geolocation(lat, longitude)
                }
            }
        }
        val ttls = cacheRegister.config["ttls"]
        edgeNSttl = if (ttls != null && ttls.has("NS")) {
            JsonUtils.optLong(ttls, "NS")
        } else {
            DEFAULT_EDGE_NS_TTL
        }
        edgeDNSRoutingLimit = JsonUtils.optInt(cr.config, "edge.dns.limit", DEFAULT_EDGE_TR_LIMIT)
        edgeHTTPRoutingLimit = JsonUtils.optInt(cr.config, "edge.http.limit", DEFAULT_EDGE_TR_LIMIT) // NOTE: this can be overridden per-DS via maxDnsAnswers
        zoneManager = ZoneManager(this, statTracker, trafficOpsUtils, trafficRouterManager)
    }

    companion object {
        val LOGGER: Logger = Logger.getLogger(TrafficRouter::class.java)

        /**
         * This is an HTTP Header the value of which, if present in a client HTTP request, should be
         * the XMLID of a Delivery Service to use as an explicit target in CLIENT_STEERING (thus
         * bypassing normal steering logic).
         */
        const val XTC_STEERING_OPTION = "x-tc-steering-option"

        /**
         * This is the key of a JSON object that is a configuration option that may be present in
         * "CRConfig" Snapshots. When this option is present, and is 'true', more Edge-Tier cache
         * servers will be provided in responses to steering requests (known as "Client Steering Forced
         * Diversity").
         */
        const val CLIENT_STEERING_DIVERSITY = "client.steering.forced.diversity"
        const val DNSSEC_ENABLED = "dnssec.enabled"
        const val DNSSEC_ZONE_DIFFING = "dnssec.zone.diffing.enabled"
        const val DNSSEC_RRSIG_CACHE_ENABLED = "dnssec.rrsig.cache.enabled"
        private const val DEFAULT_EDGE_NS_TTL: Long = 3600
        private const val DEFAULT_EDGE_TR_LIMIT = 4
        private val GEO_ZERO_ZERO = Geolocation(0.0, 0.0)
    }

    /**
     * Returns a [List] of all of the online [Cache]s that support the specified
     * [DeliveryService]. If no online caches are found to support the specified
     * DeliveryService an empty list is returned.
     *
     * @param ds
     * the DeliveryService to check
     * @return collection of supported caches
     */
    fun getSupportingCaches(caches: List<Cache>, ds: DeliveryService, requestVersion: IPVersions?): MutableList<Cache?> {
        val supportingCaches: MutableList<Cache?> = ArrayList()
        for (cache in caches) {
            if (!cache.hasDeliveryService(ds.id)) {
                continue
            }
            if (!cache.hasAuthority() || cache.isAvailable(requestVersion)) {
                supportingCaches.add(cache)
            }
        }
        return supportingCaches
    }

    /**
     * Selects a Delivery Service to service a request.
     *
     * @param request The request being served
     * @return A Delivery Service to use when servicing the request.
     */
    fun selectDeliveryService(request: Request?): DeliveryService? {
        if (cacheRegister == null) {
            LOGGER.warn("no caches yet")
            return null
        }
        val deliveryService = cacheRegister.getDeliveryService(request)
        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Selected DeliveryService: $deliveryService")
        }
        return deliveryService
    }

    /**
     * Sets the cache server and Delivery Service "states" based on input JSON.
     *
     *
     * The input `states` is expected to be an object with (at least) two keys:
     * "caches" and "deliveryServices", which contain the states of the cache servers and
     * Delivery Services, respectively. @see #setDsStates(JsonNode) and
     * [.setCacheStates] for the expected format of those objects themselves.
     *
     * @return Always returns `true` when successful. On failure, throws.
     */
    @Throws(UnknownHostException::class)
    fun setState(states: JsonNode): Boolean {
        setCacheStates(states["caches"])
        setDsStates(states["deliveryServices"])
        return true
    }

    /**
     * Sets Delivery Service states based on the input JSON.
     *
     *
     * Delivery Services present in the input which aren't registered are ignored.
     *
     * @param dsStates The input JSON object. Expected to be a map of Delivery Service XMLIDs to
     * "state" strings.
     * @return `false` iff dsStates was `null`, otherwise `true`.
     */
    private fun setDsStates(dsStates: JsonNode?): Boolean {
        if (dsStates == null) {
            return false
        }
        val dsMap = cacheRegister!!.deliveryServices
        for (dsName in dsMap.keys) {
            dsMap[dsName]!!.setState(dsStates[dsName])
        }
        return true
    }

    /**
     * Sets [Cache] states based on the input JSON.
     *
     *
     * Caches present in the input which are not registered are ignored.
     *
     * @param cacheStates The input JSON object. Expected to be a map of identifying Cache names
     * to "state" strings.
     * @return `false` iff cacheStates was `null`, otherwise `true`.
     */
    private fun setCacheStates(cacheStates: JsonNode?): Boolean {
        if (cacheStates == null) {
            return false
        }
        val cacheMap = cacheRegister!!.cacheMap ?: return false
        for (cacheName in cacheMap.keys) {
            val monitorCacheName = cacheName.replaceFirst("@.*".toRegex(), "")
            val state = cacheStates[monitorCacheName]
            cacheMap[cacheName]!!.setState(state)
        }
        return true
    }

    /**
     * Geo-locates the client returning a physical location for routing purposes.
     *
     * @param clientIP The client's network location - as a [String]. This should ideally be
     * an IP address, but trailing port number specifications are stripped.
     * @throws GeolocationException if the client could not be located.
     */
    @Throws(GeolocationException::class)
    fun getLocation(clientIP: String?): Geolocation {
        return if (clientIP?.contains(":") == true) geolocationService6.location(clientIP) else geolocationService.location(clientIP)
    }

    /**
     * Retrieves a service for geo-locating clients for a specific Delivery Service.
     *
     * @param geolocationProvider The name of the provider for geo-location information (currently
     * only "Maxmind" and "Neustar" are supported)
     * @param deliveryServiceId Currently only used for logging error information, should be an
     * identifier for a Delivery Service
     * @return A [GeolocationService] that can be used to geo-locate clients *or*
     * `null` if an error occurs.
     */
    private fun getGeolocationService(geolocationProvider: String?, deliveryServiceId: String?): GeolocationService? {
        if (applicationContext == null) {
            LOGGER.error("ApplicationContext not set unable to use custom geolocation service providers")
            return null
        }
        if (geolocationProvider == null || geolocationProvider.isEmpty()) {
            return null
        }
        try {
            return applicationContext!!.getBean(geolocationProvider) as GeolocationService
        } catch (e: Exception) {
            var error = StringBuilder("Failed getting providing class '$geolocationProvider' for geolocation")
            if (deliveryServiceId != null && deliveryServiceId.isNotEmpty()) {
                error = error.append(" for delivery service $deliveryServiceId")
            }
            error = error.append(" falling back to " + MaxmindGeolocationService::class.java.simpleName)
            LOGGER.error(error)
        }
        return null
    }

    /**
     * Retrieves a location for a given client being served a given Delivery Service using a
     * specific provider.
     * @param clientIP The client's network location - as a [String]. This should ideally be
     * an IP address, but trailing port number specifications are stripped.
     * @param geolocationProvider The name of the provider for geo-location information (currently
     * only "Maxmind" and "Neustar" are supported)
     * @param deliveryServiceId Currently only used for logging error information, should be an
     * identifier for a Delivery Service
     * @throws GeolocationException if the client could not be located.
     */
    @Throws(GeolocationException::class)
    fun getLocation(clientIP: String?, geolocationProvider: String?, deliveryServiceId: String?): Geolocation {
        val customGeolocationService = getGeolocationService(geolocationProvider, deliveryServiceId)
        return if (customGeolocationService != null) customGeolocationService.location(clientIP) else getLocation(clientIP)
    }

    /**
     * Retrieves a location for a given client being served a given Delivery Service.
     * @param clientIP The client's network location - as a [String]. This should ideally be
     * an IP address, but trailing port number specifications are stripped.
     * @param deliveryService The Delivery Service being served to the client.
     * @throws GeolocationException if the client could not be located.
     */
    @Throws(GeolocationException::class)
    fun getLocation(clientIP: String?, deliveryService: DeliveryService?): Geolocation {
        return getLocation(clientIP, deliveryService?.geolocationProvider, deliveryService?.id)
    }

    /**
     * Gets a [List] of [Cache]s that are capabable of serving a given Delivery Service.
     *
     *
     * The caches chosen are from the closest, non-empty, cache location to the client's physical
     * location, up to the Location Limit ([DeliveryService.getLocationLimit]) of the
     * Delivery Service being served.
     *
     * @param ds The Delivery Service being served.
     * @param clientLocation The physical location of the requesting client.
     * @param track The [Track] object on which a result location shall be set, should one be found
     * @return A [List] of [Cache]s that should be used to service a request should such a collection be found, or
     * `null` if the no applicable [Cache]s could be found.
     */
    @Throws(GeolocationException::class)
    fun getCachesByGeo(ds: DeliveryService, clientLocation: Geolocation?, track: Track, requestVersion: IPVersions): MutableList<Cache?>? {
        var locationsTested = 0
        val locationLimit = ds.locationLimit
        val geoEnabledCacheLocations = filterEnabledLocations(cacheRegister!!.cacheLocations, LocalizationMethod.GEO)
        val cacheLocations1 = ds.filterAvailableLocations(geoEnabledCacheLocations)
        val cacheLocations = orderLocations(cacheLocations1, clientLocation)
        for (location in cacheLocations) {
            val caches = selectCaches(location as CacheLocation, ds, requestVersion)
            if (caches != null) {
                track.setResultLocation(location.geolocation)
                if (track.getResultLocation() == GEO_ZERO_ZERO) {
                    LOGGER.error("Location " + location.id + " has Geolocation " + location.geolocation)
                }
                return caches
            }
            locationsTested++
            if (locationLimit != 0 && locationsTested >= locationLimit) {
                return null
            }
        }
        return null
    }
    /**
     * Selects [Cache]s to serve a request for a Delivery Service.
     *
     *
     * This is equivalent to calling
     * [.selectCaches] with the "deep" parameter
     * set to `true`.
     *
     * @param request The HTTP request made by the client.
     * @param ds The Delivery Service being served.
     * @param track The [Track] object that tracks how requests are served
     * @param enableDeep Sets whether or not "Deep Caching" may be used.
     */
    @JvmOverloads
    @Throws(GeolocationException::class)
    fun selectCaches(request: HTTPRequest, ds: DeliveryService, track: Track, enableDeep: Boolean = true): MutableList<Cache?>? {
        var cacheLocation: CacheLocation?
        var result = ResultType.CZ
        val useDeep = enableDeep && ds.deepCache == DeliveryService.DeepCachingType.ALWAYS
        val requestVersion = if (request.clientIP.contains(":")) IPVersions.IPV6ONLY else IPVersions.IPV4ONLY
        if (useDeep) {
            // Deep caching is enabled. See if there are deep caches available
            cacheLocation = getDeepCoverageZoneCacheLocation(request.clientIP, ds, requestVersion)
            if (cacheLocation != null && cacheLocation.caches.size != 0) {
                // Found deep caches for this client, and there are caches that might be available there.
                result = ResultType.DEEP_CZ
            } else {
                // No deep caches for this client, would have used them if there were any. Fallback to regular CZ
                cacheLocation = getCoverageZoneCacheLocation(request.clientIP, ds, requestVersion)
            }
        } else {
            // Deep caching not enabled for this Delivery Service; use the regular CZ
            cacheLocation = getCoverageZoneCacheLocation(request.clientIP, ds, false, track, requestVersion)
        }
        var caches = selectCachesByCZ(ds, cacheLocation, track, result, requestVersion)
        if (caches != null) {
            return caches
        }
        if (ds.isCoverageZoneOnly) {
            if (ds.geoRedirectUrl != null) {
                //use the NGB redirect
                caches = enforceGeoRedirect(track, ds, request.clientIP, null, requestVersion)
            } else {
                track.setResult(ResultType.MISS)
                track.setResultDetails(ResultDetails.DS_CZ_ONLY)
            }
        } else if (track.continueGeo) {
            // continue Geo can be disabled when backup group is used -- ended up an empty cache list if reach here
            caches = selectCachesByGeo(request.clientIP, ds, cacheLocation, track, requestVersion)
        }
        return caches
    }

    /**
     * Returns whether or not a Delivery Service has a valid miss location.
     * @param deliveryService The Delivery Service being served.
     */
    fun isValidMissLocation(deliveryService: DeliveryService): Boolean {
        return deliveryService.missLocation != null && deliveryService.missLocation.latitude != 0.0 && deliveryService.missLocation.longitude != 0.0
    }

    /**
     * Selects [Cache]s to serve a request for a Delivery Service based on a given location.
     * @param clientIp The requesting client's IP address - as a String.
     * @param deliveryService The Delivery Service being served.
     * @param cacheLocation A selected [CacheLocation] from which [Cache]s will be
     * extracted based on the client's location.
     * @param track The [Track] object that tracks how requests are served
     */
    @Throws(GeolocationException::class)
    fun selectCachesByGeo(clientIp: String, deliveryService: DeliveryService, cacheLocation: CacheLocation?, track: Track, requestVersion: IPVersions): MutableList<Cache?>? {
        var clientLocation: Geolocation? = null
        try {
            clientLocation = getClientLocation(clientIp, deliveryService, cacheLocation, track)
        } catch (e: GeolocationException) {
            LOGGER.warn("Failed looking up Client GeoLocation: " + e.message)
        }
        if (clientLocation == null) {
            return if (deliveryService.geoRedirectUrl != null) {
                //will use the NGB redirect
                LOGGER.debug(String.format("client is blocked by geolimit, use the NGB redirect url: %s",
                        deliveryService.geoRedirectUrl))
                enforceGeoRedirect(track, deliveryService, clientIp, track.getClientGeolocation(), requestVersion)
            } else {
                track.setResultDetails(ResultDetails.DS_CLIENT_GEO_UNSUPPORTED)
                null
            }
        }
        track.setResult(ResultType.GEO)
        if (clientLocation.isDefaultLocation && defaultGeoLocationsOverride.containsKey(clientLocation.countryCode)) {
            if (isValidMissLocation(deliveryService)) {
                clientLocation = deliveryService.missLocation
                track.setResult(ResultType.GEO_DS)
            } else {
                clientLocation = defaultGeoLocationsOverride[clientLocation.countryCode]
            }
        }
        val caches = getCachesByGeo(deliveryService, clientLocation, track, requestVersion)
        if (caches == null || caches.isEmpty()) {
            track.setResultDetails(ResultDetails.GEO_NO_CACHE_FOUND)
        }
        return caches
    }

    /**
     * Routes a single DNS request.
     * @param request The client request being routed.
     * @param track A "tracker" that tracks the results of routing.
     * @return The final result of routing.
     */
    @Throws(GeolocationException::class)
    fun route(request: DNSRequest, track: Track): DNSRouteResult? {
        val ds = selectDeliveryService(request)
        track.setRouteType(RouteType.DNS, request.hostname)

        // TODO: getHostname or getName -- !ds.getRoutingName().equalsIgnoreCase(request.getHostname().split("\\.")[0]))
        return if (ds != null && ds.isDns && request.name.toString().lowercase().matches(Regex("${ds.routingName.lowercase()}\\..*"))) {
            getEdgeCaches(request, ds, track)
        } else {
            getEdgeTrafficRouters(request, ds, track)
        }
    }

    @Throws(GeolocationException::class)
    private fun getEdgeTrafficRouters(request: DNSRequest, ds: DeliveryService?, track: Track): DNSRouteResult {
        val result = DNSRouteResult()
        result.deliveryService = ds
        result.addresses = selectTrafficRouters(request, ds, track)
        return result
    }

    @Throws(GeolocationException::class)
    private fun selectTrafficRouters(request: DNSRequest, ds: DeliveryService?, track: Track? = null): List<InetRecord> {
        val result: MutableList<InetRecord> = ArrayList()
        var resultType: ResultType?
        track?.setResultDetails(ResultDetails.LOCALIZED_DNS)
        val clientGeolocation: Geolocation?
        val networkNode = getNetworkNode(request.clientIP)
        if (networkNode != null && networkNode.geolocation != null) {
            clientGeolocation = networkNode.geolocation
            resultType = ResultType.CZ
        } else {
            clientGeolocation = getClientGeolocation(request.clientIP, track, ds)
            resultType = ResultType.GEO
        }
        if (clientGeolocation == null) {
            result.addAll(selectTrafficRoutersMiss(request.zoneName, ds))
            resultType = ResultType.MISS
        } else {
            result.addAll(selectTrafficRoutersLocalized(clientGeolocation, request.zoneName, ds, track))
            track?.setClientGeolocation(clientGeolocation)
        }
        track?.setResult(resultType)
        return result
    }

    @Throws(GeolocationException::class)
    fun selectTrafficRoutersMiss(zoneName: String?, ds: DeliveryService?): List<InetRecord> {
        val trafficRouterRecords: MutableList<InetRecord> = ArrayList()
        if (!isEdgeDNSRouting && !isEdgeHTTPRouting) {
            return trafficRouterRecords
        }
        val trafficRouterLocations = cacheRegister!!.edgeTrafficRouterLocations
        val edgeTrafficRouters: MutableList<Node> = ArrayList()
        val orderedNodes: MutableMap<String, List<Node>> = HashMap()
        var limit = if (edgeDNSRoutingLimit > getEdgeHTTPRoutingLimit(ds)) edgeDNSRoutingLimit else getEdgeHTTPRoutingLimit(ds)
        var index = 0
        var exhausted = false

        // if limits don't exist, or do exist and are higher than the number of edge TRs, use the number of edge TRs as the limit
        if (limit == 0 || limit > cacheRegister.edgeTrafficRouterCount) {
            limit = cacheRegister.edgeTrafficRouterCount
        }

        // grab one TR per location until the limit is reached
        while (edgeTrafficRouters.size < limit && !exhausted) {
            val initialCount = edgeTrafficRouters.size
            for (location in trafficRouterLocations) {
                if (edgeTrafficRouters.size >= limit) {
                    break
                }
                if (!orderedNodes.containsKey(location.id)) {
                    orderedNodes[location.id] = consistentHasher.selectHashables(location.trafficRouters, zoneName)
                }
                val trafficRouters = orderedNodes[location.id]
                if (trafficRouters == null || trafficRouters.isEmpty() || index >= trafficRouters.size) {
                    continue
                }
                edgeTrafficRouters.add(trafficRouters[index])
            }

            /*
			 * we iterated through every location and attempted to add edge TR at index, but none were added....
			 * normally, these values would never match unless we ran out of options...
			 * if so, we've exhausted our options so we need to break out of the while loop
			 */if (initialCount == edgeTrafficRouters.size) {
                exhausted = true
            }
            index++
        }
        if (edgeTrafficRouters.isNotEmpty()) {
            if (isEdgeDNSRouting) {
                trafficRouterRecords.addAll(nsRecordsFromNodes(ds, edgeTrafficRouters))
            }
            if (ds != null && !ds.isDns && isEdgeHTTPRouting) { // only generate edge routing records for HTTP DSs when necessary
                trafficRouterRecords.addAll(inetRecordsFromNodes(ds, edgeTrafficRouters))
            }
        }
        return trafficRouterRecords
    }

    @JvmOverloads
    @Throws(GeolocationException::class)
    fun selectTrafficRoutersLocalized(clientGeolocation: Geolocation?, zoneName: String?, ds: DeliveryService?, track: Track? = null): List<InetRecord> {
        val trafficRouterRecords: MutableList<InetRecord> = ArrayList()
        if (!isEdgeDNSRouting && !isEdgeHTTPRouting) {
            return trafficRouterRecords
        }
        val trafficRouterLocations = orderLocations(cacheRegister!!.edgeTrafficRouterLocations, clientGeolocation)
        for (location in trafficRouterLocations) {
            location as TrafficRouterLocation
            val trafficRouters = consistentHasher.selectHashables(location.trafficRouters, zoneName)
            if (trafficRouters == null || trafficRouters.isEmpty()) {
                continue
            }
            if (isEdgeDNSRouting) {
                trafficRouterRecords.addAll(nsRecordsFromNodes(ds, trafficRouters))
            }
            if (ds != null && !ds.isDns && isEdgeHTTPRouting) { // only generate edge routing records for HTTP DSs when necessary
                trafficRouterRecords.addAll(inetRecordsFromNodes(ds, trafficRouters))
            }
            track?.setResultLocation(location.geolocation)
            break
        }
        return trafficRouterRecords
    }

    @Throws(GeolocationException::class)
    private fun getEdgeCaches(request: DNSRequest, ds: DeliveryService?, track: Track): DNSRouteResult? {
        val result = DNSRouteResult()
        result.deliveryService = ds
        if (ds == null) {
            track.setResult(ResultType.STATIC_ROUTE)
            track.setResultDetails(ResultDetails.DS_NOT_FOUND)
            return null
        }
        if (!ds.isAvailable) {
            result.addresses = ds.getFailureDnsResponse(request, track)
            result.addAddresses(selectTrafficRouters(request, ds))
            return result
        }
        val requestVersion = if (request.queryType == Type.AAAA) IPVersions.IPV6ONLY else IPVersions.IPV4ONLY
        val cacheLocation = getCoverageZoneCacheLocation(request.clientIP, ds, false, track, requestVersion)
        var caches = selectCachesByCZ(ds, cacheLocation, track, requestVersion)
        if (caches != null) {
            track.setResult(ResultType.CZ)
            track.setClientGeolocation(cacheLocation!!.geolocation)
            result.addresses = inetRecordsFromCaches(ds, caches, request)
            result.addAddresses(selectTrafficRouters(request, ds))
            return result
        }
        if (ds.isCoverageZoneOnly) {
            track.setResult(ResultType.MISS)
            track.setResultDetails(ResultDetails.DS_CZ_ONLY)
            result.addresses = ds.getFailureDnsResponse(request, track)
            result.addAddresses(selectTrafficRouters(request, ds))
            return result
        }
        try {
            val inetRecords = federationRegistry.findInetRecords(ds.id, CidrAddress.fromString(request.clientIP))
            if (inetRecords != null && inetRecords.isNotEmpty()) {
                result.addresses = inetRecords
                track.setResult(ResultType.FED)
                return result
            }
        } catch (e: NetworkNodeException) {
            LOGGER.error("Bad client address: '" + request.clientIP + "'")
        }
        if (track.continueGeo) {
            caches = selectCachesByGeo(request.clientIP, ds, cacheLocation, track, requestVersion)
        }
        if (caches != null) {
            track.setResult(ResultType.GEO)
            result.addresses = inetRecordsFromCaches(ds, caches, request)
        } else {
            track.setResult(ResultType.MISS)
            result.addresses = ds.getFailureDnsResponse(request, track)
        }
        result.addAddresses(selectTrafficRouters(request, ds))
        return result
    }

    private fun nsRecordsFromNodes(ds: DeliveryService?, nodes: List<Node>): List<InetRecord> {
        val nsRecords: MutableList<InetRecord> = ArrayList()
        val limit = if (edgeDNSRoutingLimit > nodes.size) nodes.size else edgeDNSRoutingLimit
        var ttl = edgeNSttl
        if (ds != null && ds.ttls.has("NS")) {
            ttl = JsonUtils.optLong(ds.ttls, "NS") // no exception
        }
        for (i in 0 until limit) {
            val node = nodes[i]
            nsRecords.add(InetRecord(node.fqdn, ttl, Type.NS))
        }
        return nsRecords
    }

    private fun inetRecordsFromNodes(ds: DeliveryService?, nodes: List<Node>): List<InetRecord> {
        val addresses: MutableList<InetRecord> = ArrayList()
        val limit = if (getEdgeHTTPRoutingLimit(ds) > nodes.size) nodes.size else getEdgeHTTPRoutingLimit(ds)
        if (ds == null) {
            return addresses
        }
        val ttls = ds.ttls
        for (i in 0 until limit) {
            val node = nodes[i]
            if (node.ip4 != null) {
                addresses.add(InetRecord(node.ip4, JsonUtils.optLong(ttls, "A")))
            }
            if (node.ip6 != null && ds.isIp6RoutingEnabled) {
                addresses.add(InetRecord(node.ip6, JsonUtils.optLong(ttls, "AAAA")))
            }
        }
        return addresses
    }

    /**
     * Extracts the IP Addresses from a set of caches based on a Delivery Service's configuration
     * @param ds The Delivery Service being served. If this DS does not have "IPv6 routing enabled",
     * then the IPAddresses returned will not include IPv6 addresses.
     * @param caches The list of caches chosen to serve ds. If the length of this list is greater
     * than the maximum allowed IP addresses in a DNS response by the
     * [DeliveryService.getMaxDnsIps] of the requested Delivery Service, the maximum
     * allowed number will be chosen from the list at random.
     * @param request The request being served - used for consistent hashing when caches must be
     * chosen at random
     * @return The IP Addresses of the passed caches. In general, these may be IPv4 or IPv6.
     */
    fun inetRecordsFromCaches(ds: DeliveryService, caches: MutableList<Cache?>, request: Request): List<InetRecord> {
        val addresses: MutableList<InetRecord> = ArrayList()
        val maxDnsIps = ds.maxDnsIps
        val selectedCaches: MutableList<Cache?>
        if (maxDnsIps > 0 && isConsistentDNSRouting) { // only consistent hash if we must
            selectedCaches = consistentHasher.selectHashables(caches, ds.dispersion, request.hostname) as MutableList<Cache?>
        } else if (maxDnsIps > 0) {
            /*
			 * We also shuffle in NameServer when adding Records to the Message prior
			 * to sending it out, as the Records are sorted later when we fill the
			 * dynamic zone if DNSSEC is enabled. We shuffle here prior to pruning
			 * for maxDnsIps so that we ensure we are spreading load across all caches
			 * assigned to this delivery service.
			*/
            caches.shuffle(random)
            selectedCaches = ArrayList()
            for (cache in caches) {
                selectedCaches.add(cache)
                if (selectedCaches.size >= maxDnsIps) {
                    break
                }
            }
        } else {
            selectedCaches = caches
        }
        for (cache in selectedCaches) {
            addresses.addAll(cache!!.getIpAddresses(ds.ttls, ds.isIp6RoutingEnabled))
        }
        return addresses
    }

    /**
     * Geo-locates the client based on their IP address and the Delivery Service they requested.
     *
     *
     * This is optimized over [.getLocation] because
     * @param clientIp The IP Address of the requesting client.
     * @param track A state-tracking object, it will be notified of the calculated client location
     * for optimization of future queries.
     * @param deliveryService The Delivery Service being served. Currently only used for logging
     * error information.
     * @return The client's calculated geographic location
     * @throws GeolocationException
     */
    @Throws(GeolocationException::class)
    fun getClientGeolocation(clientIp: String, track: Track?, deliveryService: DeliveryService?): Geolocation {
        if (track != null && track.isClientGeolocationQueried()) {
            return track.getClientGeolocation()
        }
        val clientGeolocation: Geolocation = deliveryService?.let { getLocation(clientIp, it) } ?: getLocation(clientIp)
        if (track != null) {
            track.setClientGeolocation(clientGeolocation)
            track.setClientGeolocationQueried(true)
        }
        return clientGeolocation
    }

    /**
     * Geo-locates the client based on their IP address and the Delivery Service they requested.
     * @param clientIp The IP Address of the requesting client.
     * @param ds The Delivery Service being served. If the client's location is blocked by this
     * Delivery Service, the returned location will instead be the appropriate fallback/miss
     * location.
     * @param cacheLocation If this is not 'null', its location will be used in lieu of calculating
     * one for the client.
     * @return The client's calculated geographic location (or the appropriate fallback/miss
     * location).
     */
    @Throws(GeolocationException::class)
    fun getClientLocation(clientIp: String, ds: DeliveryService, cacheLocation: Location?, track: Track?): Geolocation {
        if (cacheLocation != null) {
            return cacheLocation.geolocation
        }
        val clientGeolocation = getClientGeolocation(clientIp, track, ds)
        return ds.supportLocation(clientGeolocation)
    }

    /**
     * Selects caches to service requests for a Delivery Service from a cache location based on
     * Coverage Zone configuration.
     *
     *
     * This is equivalent to calling [.selectCachesByCZ]
     * with a 'null' "track" argument.
     *
     * @param ds The Delivery Service being served.
     * @param cacheLocation The location from which caches will be selected.
     * @return All of the caches in the given location capable of serving ds.
     */
    fun selectCachesByCZ(ds: DeliveryService, cacheLocation: CacheLocation?, requestVersion: IPVersions): List<Cache?>? {
        return selectCachesByCZ(ds, cacheLocation, null, requestVersion)
    }

    /**
     * Selects caches to service requests for a Delivery Service from a cache location based on
     * Coverage Zone Configuration.
     * @param deliveryServiceId An identifier for the [DeliveryService] being served.
     * @param cacheLocationId An identifier for the [CacheLocation] from which caches will be
     * selected.
     * @return All of the caches in the given location capable of serving the identified Delivery
     * Service.
     */
    fun selectCachesByCZ(deliveryServiceId: String?, cacheLocationId: String?, track: Track?, requestVersion: IPVersions): List<Cache?>? {
        return selectCachesByCZ(cacheRegister!!.getDeliveryService(deliveryServiceId), cacheRegister.getCacheLocation(cacheLocationId), track, requestVersion)
    }

    /**
     * Selects caches to service requests for a Delivery Service from a cache location based on
     * Coverage Zone Configuration.
     *
     *
     * This is equivalent to calling [.selectCachesByCZ]
     * with the "result" argument set to [ResultType.CZ].
     *
     * @param ds The Delivery Service being served.
     * @param cacheLocation The location from which caches will be selected
     * @return All of the caches in the given location capable of serving ds.
     */
    private fun selectCachesByCZ(ds: DeliveryService, cacheLocation: CacheLocation?, track: Track?, requestVersion: IPVersions): MutableList<Cache?>? {
        return selectCachesByCZ(ds, cacheLocation, track, ResultType.CZ, requestVersion) // ResultType.CZ was the original default before DDC
    }

    /**
     * Selects caches to service requests for a Delivery Service from a cache location based on
     * Coverage Zone Configuration.
     *
     *
     * Obviously, at this point, the location from which to select caches must already be known.
     * So it's totally possible that that decision wasn't made based on Coverage Zones at all,
     * that's just the default routing result chosen by a common caller of this method
     * ([.selectCachesByCZ]).
     *
     * @param ds The Delivery Service being served.
     * @param cacheLocation The location from which caches will be selected.
     * @param result The type of routing result that resulted in the returned caches being selected.
     * This is used for tracking routing results.
     * @return All of the caches in the given location capable of serving ds.
     */
    private fun selectCachesByCZ(ds: DeliveryService?, cacheLocation: CacheLocation?, track: Track?, result: ResultType, requestVersion: IPVersions): MutableList<Cache?>? {
        if (cacheLocation == null || ds == null || !ds.isLocationAvailable(cacheLocation)) {
            return null
        }
        val caches = selectCaches(cacheLocation, ds, requestVersion)
        if (caches != null && track != null) {
            track.setResult(result)
            if (track.isFromBackupCzGroup) {
                track.setResultDetails(ResultDetails.DS_CZ_BACKUP_CG)
            }
            track.setResultLocation(cacheLocation.geolocation)
        }
        return caches
    }

    /**
     * Gets multiple routes for STEERING Delivery Services
     *
     * @param request The client's HTTP Request
     * @param track A [Track] object used to track routing statistics
     * @return The list of routes available to service the client's request.
     */
    @Throws(MalformedURLException::class, GeolocationException::class)
    fun multiRoute(request: HTTPRequest, track: Track): HTTPRouteResult? {
        val entryDeliveryService = cacheRegister!!.getDeliveryService(request)
        val steeringResults = getSteeringResults(request, track, entryDeliveryService) ?: return null
        val routeResult = HTTPRouteResult(true)
        routeResult.deliveryService = entryDeliveryService
        if (entryDeliveryService.isRegionalGeoEnabled) {
            RegionalGeo.enforce(this, request, entryDeliveryService, null, routeResult, track)
            if (routeResult.url != null) {
                return routeResult
            }
        }
        val resultsToRemove: MutableList<SteeringResult> = ArrayList()
        val selectedCaches: MutableSet<Cache?> = HashSet()

        // Pattern based consistent hashing - use consistentHashRegex from steering DS instead of targets
        val steeringHash = buildPatternBasedHashString(entryDeliveryService.consistentHashRegex, request.path)
        for (steeringResult in steeringResults) {
            val ds = steeringResult.deliveryService
            var caches: List<Cache?>? = selectCaches(request, ds, track)

            // child Delivery Services can use their query parameters
            val pathToHash = steeringHash + ds.extractSignificantQueryParams(request)
            if (caches != null && caches.isNotEmpty()) {
                if (isClientSteeringDiversityEnabled) {
                    var tryCaches: MutableList<Cache?>? = ArrayList(caches)
                    tryCaches!!.removeAll(selectedCaches)
                    if (tryCaches.isNotEmpty()) {
                        caches = tryCaches
                    } else if (track.result == ResultType.DEEP_CZ) {
                        // deep caches have been selected already, try non-deep selection
                        tryCaches = selectCaches(request, ds, track, false)
                        track.setResult(ResultType.DEEP_CZ) // request should still be tracked as a DEEP_CZ hit
                        tryCaches!!.removeAll(selectedCaches)
                        if (tryCaches.isNotEmpty()) {
                            caches = tryCaches
                        }
                    }
                }
                val cache = consistentHasher.selectHashable(caches, ds.dispersion, pathToHash)
                steeringResult.cache = cache
                selectedCaches.add(cache)
            } else {
                resultsToRemove.add(steeringResult)
            }
        }
        steeringResults.removeAll(resultsToRemove)
        geoSortSteeringResults(steeringResults, request.clientIP, entryDeliveryService)
        for (steeringResult in steeringResults) {
            routeResult.addUrl(URL(steeringResult.deliveryService.createURIString(request, steeringResult.cache)))
        }
        if (routeResult.urls.isEmpty()) {
            routeResult.addUrl(entryDeliveryService.getFailureHttpResponse(request, track))
        }
        return routeResult
    }

    /**
     * Creates a string to be used in consistent hashing.
     *
     *
     * This uses simply the request path by default, but will consider any and all Query Parameters
     * that are in deliveryService's [DeliveryService] consistentHashQueryParam set as well.
     * It will also fall back on the request path if the query parameters are not UTF-8-encoded.
     *
     * @param deliveryService The [DeliveryService] being requested
     * @param request An [HTTPRequest] representing the client's request.
     * @return A string appropriate to use for consistent hashing to service the request
     */
    fun buildPatternBasedHashString(deliveryService: DeliveryService, request: HTTPRequest): String {
        val requestPath = request.path
        val hashString = StringBuilder("")
        if (deliveryService.consistentHashRegex != null && requestPath.isNotEmpty()) {
            hashString.append(buildPatternBasedHashString(deliveryService.consistentHashRegex, requestPath))
        }
        hashString.append(deliveryService.extractSignificantQueryParams(request))
        return hashString.toString()
    }

    /**
     * Constructs a string to be used in consistent hashing
     *
     *
     * If `regex` is `null` or empty - or if an error occurs applying it -, returns
     * `requestPath` unaltered.
     *
     * @param regex A regular expression matched against the client's request path to extract
     * information important to consistent hashing
     * @param requestPath The client's request path - e.g. `/some/path` from
     * `https://example.com/some/path`
     * @return The parts of requestPath that matched regex
     */
    fun buildPatternBasedHashString(regex: String?, requestPath: String): String {
        if (regex == null || regex.isEmpty()) {
            return requestPath
        }
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(requestPath)
            val sb = StringBuilder("")
            if (matcher.find() && matcher.groupCount() > 0) {
                for (i in 1..matcher.groupCount()) {
                    val text = matcher.group(i)
                    sb.append(text)
                }
                return sb.toString()
            }
        } catch (e: Exception) {
            val error = StringBuilder("Failed to construct hash string using regular expression: '")
            error.append(regex)
            error.append("' against request path: '")
            error.append(requestPath)
            error.append("' Exception: ")
            error.append(e.toString())
            LOGGER.error(error.toString())
        }
        return requestPath
    }

    /**
     * Routes an HTTP request.
     * @param request The request being routed.
     * @return The result of routing the HTTP request.
     * @throws MalformedURLException
     * @throws GeolocationException
     */
    @Throws(MalformedURLException::class, GeolocationException::class)
    fun route(request: HTTPRequest, track: Track): HTTPRouteResult? {
        track.setRouteType(RouteType.HTTP, request.hostname)
        return if (isMultiRouteRequest(request)) {
            multiRoute(request, track)
        } else {
            singleRoute(request, track)
        }
    }

    /**
     * Routes an HTTP request that isn't for a CLIENT_STEERING-type Delivery Service.
     * @param request The request being routed.
     * @return The result of routing this HTTP request.
     * @throws MalformedURLException if a URL cannot be constructed to return to the client
     */
    @Throws(MalformedURLException::class, GeolocationException::class)
    fun singleRoute(request: HTTPRequest, track: Track): HTTPRouteResult? {
        val deliveryService = getDeliveryService(request, track) ?: return null
        val routeResult = HTTPRouteResult(false)
        if (!deliveryService.isAvailable) {
            routeResult.url = deliveryService.getFailureHttpResponse(request, track)
            return routeResult
        }
        routeResult.deliveryService = deliveryService
        val caches: List<Cache?>? = selectCaches(request, deliveryService, track)
        if (caches == null || caches.isEmpty()) {
            if (track.getResult() == ResultType.GEO_REDIRECT) {
                routeResult.url = URL(deliveryService.geoRedirectUrl)
                LOGGER.debug(String.format("NGB redirect to url: %s for request: %s", deliveryService.geoRedirectUrl, request.requestedUrl))
                return routeResult
            }
            routeResult.url = deliveryService.getFailureHttpResponse(request, track)
            return routeResult
        }

        // Pattern based consistent hashing
        val pathToHash = buildPatternBasedHashString(deliveryService, request)
        val cache = consistentHasher.selectHashable(caches, deliveryService.dispersion, pathToHash)

        // Enforce anonymous IP blocking if a DS has anonymous blocking enabled
        // and the feature is enabled
        if (deliveryService.isAnonymousIpEnabled && AnonymousIp.getCurrentConfig().enabled) {
            AnonymousIp.enforce(this, request, deliveryService, cache, routeResult, track)
            if (routeResult.responseCode == AnonymousIp.BLOCK_CODE) {
                return routeResult
            }
        }
        if (deliveryService.isRegionalGeoEnabled) {
            RegionalGeo.enforce(this, request, deliveryService, cache, routeResult, track)
            return routeResult
        }
        val uriString = deliveryService.createURIString(request, cache)
        routeResult.url = URL(uriString)
        return routeResult
    }

    /**
     * Gets all the possible steering results for a request to a Delivery Service.
     * @param request The client HTTP request.
     * @param entryDeliveryService The steering Delivery Service being served.
     * @return All of the possible steering results for routing request through entryDeliveryService.
     */
    private fun getSteeringResults(request: HTTPRequest, track: Track, entryDeliveryService: DeliveryService): MutableList<SteeringResult>? {
        if (isTlsMismatch(request, entryDeliveryService)) {
            track.setResult(ResultType.ERROR)
            track.setResultDetails(ResultDetails.DS_TLS_MISMATCH)
            return null
        }
        val steeringResults = consistentHashMultiDeliveryService(entryDeliveryService, request)
        if (steeringResults == null || steeringResults.isEmpty()) {
            track.setResult(ResultType.DS_MISS)
            track.setResultDetails(ResultDetails.DS_NOT_FOUND)
            return null
        }
        val toBeRemoved: MutableList<SteeringResult> = ArrayList()
        for (steeringResult in steeringResults) {
            val ds = steeringResult.deliveryService
            if (isTlsMismatch(request, ds)) {
                track.setResult(ResultType.ERROR)
                track.setResultDetails(ResultDetails.DS_TLS_MISMATCH)
                return null
            }
            if (!ds.isAvailable) {
                toBeRemoved.add(steeringResult)
            }
        }
        steeringResults.removeAll(toBeRemoved)
        return if (steeringResults.isEmpty()) null else steeringResults
    }

    /**
     * Gets the Delivery Service that matches the client HTTP request.
     * @param request The client HTTP request.
     * @return The Delivery Service corresponding to the request if one can be found, otherwise
     * `null`.
     */
    private fun getDeliveryService(request: HTTPRequest, track: Track): DeliveryService? {
        val xtcSteeringOption = request.getHeaderValue(XTC_STEERING_OPTION)
        val deliveryService = consistentHashDeliveryService(cacheRegister!!.getDeliveryService(request), request, xtcSteeringOption)
        if (deliveryService == null) {
            track.setResult(ResultType.DS_MISS)
            track.setResultDetails(ResultDetails.DS_NOT_FOUND)
            return null
        }
        if (isTlsMismatch(request, deliveryService)) {
            track.setResult(ResultType.ERROR)
            track.setResultDetails(ResultDetails.DS_TLS_MISMATCH)
            return null
        }
        return deliveryService
    }

    /**
     * Checks if the TLS settings on the client HTTP request match those of the Delivery Service
     * it's requesting.
     * @param request The client HTTP request.
     * @param deliveryService The Delivery Service being served.
     */
    private fun isTlsMismatch(request: HTTPRequest, deliveryService: DeliveryService): Boolean {
        if (request.isSecure && !deliveryService.isSslEnabled) {
            return true
        }
        return !request.isSecure && !deliveryService.isAcceptHttp
    }

    /**
     * Finds a network subnet for the given IP address based on Deep Coverage Zone configuration.
     * @param ip The IP address to look up.
     * @return A network subnet  capable of serving requests for the given IP, or `null` if
     * one couldn't be found.
     */
    private fun getDeepNetworkNode(ip: String?): NetworkNode? {
        try {
            return NetworkNode.getDeepInstance().getNetwork(ip)
        } catch (e: NetworkNodeException) {
            LOGGER.warn(e)
        }
        return null
    }

    /**
     * Finds a network subnet for the given IP address based on Coverage Zone configuration.
     * @param ip The IP address to look up.
     * @return A network subnet capable of serving requests for the given IP, or `null` if
     * one couldn't be found.
     */
    private fun getNetworkNode(ip: String?): NetworkNode? {
        try {
            return NetworkNode.getInstance().getNetwork(ip)
        } catch (e: NetworkNodeException) {
            LOGGER.warn(e)
        }
        return null
    }

    fun getCoverageZoneCacheLocation(ip: String, deliveryServiceId: String, requestVersion: IPVersions?): CacheLocation? {
        return getCoverageZoneCacheLocation(ip, deliveryServiceId, false, null, requestVersion) // default is not deep
    }

    /**
     * Finds the deep coverage zone location information for a give IP address.
     * @param ip
     * @return deep coverage zone location
     */
    fun getDeepCoverageZoneLocationByIP(ip: String?): CacheLocation? {
        val networkNode = getDeepNetworkNode(ip) ?: return null
        val cacheLocation = networkNode.location as CacheLocation
        cacheLocation.loadDeepCaches(networkNode.deepCacheNames, cacheRegister)
        return cacheLocation
    }

    fun getCoverageZoneCacheLocation(ip: String, deliveryServiceId: String, useDeep: Boolean, track: Track?, requestVersion: IPVersions?): CacheLocation? {
        val networkNode = if (useDeep) getDeepNetworkNode(ip) else getNetworkNode(ip)
        val localizationMethod = if (useDeep) LocalizationMethod.DEEP_CZ else LocalizationMethod.CZ
        if (networkNode == null) {
            return null
        }
        val deliveryService = cacheRegister!!.getDeliveryService(deliveryServiceId)
        var cacheLocation: CacheLocation? = networkNode.location as CacheLocation
        if (useDeep && cacheLocation != null) {
            // lazily load deep Caches into the deep CacheLocation
            cacheLocation.loadDeepCaches(networkNode.deepCacheNames, cacheRegister)
        }
        if (cacheLocation != null && !cacheLocation.isEnabledFor(localizationMethod)) {
            return null
        }
        if (cacheLocation != null && getSupportingCaches(cacheLocation.caches, deliveryService, requestVersion).isNotEmpty()) {
            return cacheLocation
        }
        if (useDeep) {
            // there were no available deep caches in the deep CZF
            return null
        }
        if (networkNode.loc == null) {
            return null
        }

        // find CacheLocation
        cacheLocation = cacheRegister.getCacheLocationById(networkNode.loc)
        if (cacheLocation != null && !cacheLocation.isEnabledFor(localizationMethod)) {
            track!!.continueGeo = false // hit in the CZF but the cachegroup doesn't allow CZ-localization, don't fall back to GEO
            return null
        }
        if (cacheLocation != null && getSupportingCaches(cacheLocation.caches, deliveryService, requestVersion).isNotEmpty()) {
            // lazy loading in case a CacheLocation has not yet been associated with this NetworkNode
            networkNode.location = cacheLocation
            return cacheLocation
        }
        if (cacheLocation != null && cacheLocation.backupCacheGroups != null) {
            for (cacheGroup in cacheLocation.backupCacheGroups) {
                val bkCacheLocation = cacheRegister.getCacheLocationById(cacheGroup)
                if (bkCacheLocation != null && !bkCacheLocation.isEnabledFor(localizationMethod)) {
                    continue
                }
                if (bkCacheLocation != null && getSupportingCaches(bkCacheLocation.caches, deliveryService, requestVersion).isNotEmpty()) {
                    LOGGER.debug("Got backup CZ cache group " + bkCacheLocation.id + " for " + ip + ", ds " + deliveryServiceId)
                    if (track != null) {
                        track.isFromBackupCzGroup = true
                    }
                    return bkCacheLocation
                }
            }
            // track.continueGeo
            // will become to false only when backups are configured and (primary group's) fallbackToClosedGeo is configured (non-empty list) to false
            // False signals subsequent cacheSelection routine to stop geo based selection.
            if (!cacheLocation.isUseClosestGeoLoc) {
                track!!.continueGeo = false
                return null
            }
        }

        // We had a hit in the CZF but the name does not match a known cache location.
        // Check whether the CZF entry has a geolocation and use it if so.
        var availableLocations = cacheRegister.filterAvailableCacheLocations(deliveryServiceId)
        availableLocations = filterEnabledLocations(availableLocations, localizationMethod)
        val closestCacheLocation = getClosestCacheLocation(availableLocations, networkNode.geolocation, cacheRegister.getDeliveryService(deliveryServiceId), requestVersion)
        if (closestCacheLocation != null) {
            LOGGER.debug("Got closest CZ cache group " + closestCacheLocation.id + " for " + ip + ", ds " + deliveryServiceId)
            if (track != null) {
                track.isFromBackupCzGroup = true
            }
        }
        return closestCacheLocation
    }

    fun filterEnabledLocations(locations: Collection<CacheLocation>, localizationMethod: LocalizationMethod?): List<CacheLocation> {
        return locations.stream()
                .filter { loc: CacheLocation -> loc.isEnabledFor(localizationMethod) }
                .collect(Collectors.toList())
    }

    private fun getDeepCoverageZoneCacheLocation(ip: String, deliveryService: DeliveryService, requestVersion: IPVersions?): CacheLocation? {
        return getCoverageZoneCacheLocation(ip, deliveryService, true, null, requestVersion)
    }

    private fun getCoverageZoneCacheLocation(ip: String, deliveryService: DeliveryService, useDeep: Boolean, track: Track?, requestVersion: IPVersions?): CacheLocation? {
        return getCoverageZoneCacheLocation(ip, deliveryService.id, useDeep, track, requestVersion)
    }

    fun getCoverageZoneCacheLocation(ip: String, deliveryService: DeliveryService, requestVersion: IPVersions?): CacheLocation? {
        return getCoverageZoneCacheLocation(ip, deliveryService.id, requestVersion)
    }

    /**
     * Chooses a [Cache] for a Delivery Service based on the Coverage Zone File given a
     * client's IP and request *path*.
     * @param ip The client's IP address
     * @param deliveryServiceId The "xml_id" of a Delivery Service being routed
     * @param requestPath The client's requested path - e.g.
     * `http://test.example.com/request/path`  `/request/path`
     * @param useDeep if `true` will attempt to use Deep Coverage Zones - otherwise will only
     * use Coverage Zone File
     * @return A cache object chosen to serve the client's request
     */
    @JvmOverloads
    fun consistentHashForCoverageZone(ip: String, deliveryServiceId: String, requestPath: String?, useDeep: Boolean = false): Cache? {
        val r = HTTPRequest()
        r.path = requestPath
        r.queryString = ""
        return consistentHashForCoverageZone(ip, deliveryServiceId, r, useDeep)
    }

    /**
     * Chooses a cache for a Delivery Service based on the Coverage Zone File or Deep Coverage Zone
     * File given a client's IP and request.
     * @param ip The client's IP address
     * @param deliveryServiceId The "xml_id" of a Delivery Service being routed
     * @param request The client's HTTP request
     * @param useDeep if `true` will attempt to use Deep Coverage Zones - otherwise will only
     * use Coverage Zone File
     * @return A [Cache] object chosen to serve the client's request
     */
    private fun consistentHashForCoverageZone(ip: String, deliveryServiceId: String, request: HTTPRequest, useDeep: Boolean): Cache? {
        val deliveryService = cacheRegister!!.getDeliveryService(deliveryServiceId)
        if (deliveryService == null) {
            LOGGER.error("Failed getting delivery service from cache register for id '$deliveryServiceId'")
            return null
        }
        val requestVersion = if (ip.contains(":")) IPVersions.IPV6ONLY else IPVersions.IPV4ONLY
        val coverageZoneCacheLocation = getCoverageZoneCacheLocation(ip, deliveryService, useDeep, null, requestVersion)
        val caches = selectCachesByCZ(deliveryService, coverageZoneCacheLocation, requestVersion)
        if (caches == null || caches.isEmpty()) {
            return null
        }
        val pathToHash = buildPatternBasedHashString(deliveryService, request)
        return consistentHasher.selectHashable(caches, deliveryService.dispersion, pathToHash)
    }

    /**
     * Chooses a [Cache] for a Delivery Service based on GeoLocation given a client's IP and
     * request *path*.
     * @param ip The client's IP address
     * @param deliveryServiceId The "xml_id" of a Delivery Service being routed
     * @param requestPath The client's requested path - e.g.
     * `http://test.example.com/request/path`  `/request/path`
     * @return A cache object chosen to serve the client's request
     */
    fun consistentHashForGeolocation(ip: String, deliveryServiceId: String, requestPath: String?): Cache? {
        val r = HTTPRequest()
        r.path = requestPath
        r.queryString = ""
        return consistentHashForGeolocation(ip, deliveryServiceId, r)
    }

    /**
     * Chooses a [Cache] for a Delivery Service based on GeoLocation given a client's IP and
     * request.
     * @param ip The client's IP address
     * @param deliveryServiceId The "xml_id" of a Delivery Service being routed
     * @param request The client's HTTP request
     * @return A cache object chosen to serve the client's request
     */
    private fun consistentHashForGeolocation(ip: String, deliveryServiceId: String, request: HTTPRequest): Cache? {
        val deliveryService = cacheRegister!!.getDeliveryService(deliveryServiceId)
        if (deliveryService == null) {
            LOGGER.error("Failed getting delivery service from cache register for id '$deliveryServiceId'")
            return null
        }
        val requestVersion = if (ip.contains(":")) IPVersions.IPV6ONLY else IPVersions.IPV4ONLY
        var caches: List<Cache?>? = null
        if (deliveryService.isCoverageZoneOnly && deliveryService.geoRedirectUrl != null) {
            //use the NGB redirect
            caches = enforceGeoRedirect(StatTracker.getTrack(), deliveryService, ip, null, requestVersion)
        } else {
            val cacheLocation = getCoverageZoneCacheLocation(ip, deliveryServiceId, requestVersion)
            try {
                caches = selectCachesByGeo(ip, deliveryService, cacheLocation, StatTracker.getTrack(), requestVersion)
            } catch (e: GeolocationException) {
                LOGGER.warn("Failed gettting list of caches by geolocation for ip $ip delivery service id '$deliveryServiceId'")
            }
        }
        if (caches == null || caches.isEmpty()) {
            return null
        }
        val pathToHash = buildPatternBasedHashString(deliveryService, request)
        return consistentHasher.selectHashable(caches, deliveryService.dispersion, pathToHash)
    }

    /**
     * Builds a string to be used for consistent hashing based on a client's request *path*.
     * @param deliveryServiceId The "xml_id" of a Delivery Service, the consistent hash settings of
     * which will be used to build the consistent hashing string.
     * @param requestPath The client's requested path.
     * @return A string suitable for using in consistent hashing.
     */
    fun buildPatternBasedHashStringDeliveryService(deliveryServiceId: String?, requestPath: String?): String {
        val r = HTTPRequest()
        r.path = requestPath
        r.queryString = ""
        return buildPatternBasedHashString(cacheRegister!!.getDeliveryService(deliveryServiceId), r)
    }

    /**
     * Returns whether or not the given Delivery Service is of the STEERING or CLIENT_STEERING type.
     */
    private fun isSteeringDeliveryService(deliveryService: DeliveryService?): Boolean {
        return deliveryService != null && steeringRegistry!!.has(deliveryService.id)
    }

    /**
     * Checks whether the given client's HTTP request is for a CLIENT_STEERING Delivery Service.
     */
    private fun isMultiRouteRequest(request: HTTPRequest): Boolean {
        val deliveryService = cacheRegister!!.getDeliveryService(request)
        return if (deliveryService == null || !isSteeringDeliveryService(deliveryService)) {
            false
        } else steeringRegistry!![deliveryService.id].isClientSteering
    }

    /**
     * Gets a geographic location for the client based on their IP address.
     * @param clientIP The client's IP address as a string.
     * @param deliveryService The Delivery Service the client is requesting. This is used to
     * determine the appropriate location if the client cannot be located, or is blocked by RGB
     * or Anonymous Blocking rules.
     * @return The client's calculated geographic location, or `null` if they cannot be
     * geo-located (and deliveryService has no default "miss" location set) or if the client is
     * blocked by the Delivery Service's settings.
     */
    fun getClientLocationByCoverageZoneOrGeo(clientIP: String?, deliveryService: DeliveryService?): Geolocation? {
        val clientLocation: Geolocation?
        val networkNode = getNetworkNode(clientIP)
        clientLocation = if (networkNode != null && networkNode.geolocation != null) {
            networkNode.geolocation
        } else {
            try {
                getLocation(clientIP, deliveryService)
            } catch (e: GeolocationException) {
                null
            }
        }
        return deliveryService?.supportLocation(clientLocation)
    }

    /**
     * Sorts the provided steering results by their geographic proximity to the client and their
     * configured ordering and weights.
     * @param steeringResults The results to be sorted. They are sorted "in place" - this modifies
     * the list directly.
     * @param clientIP The client's IP address as a string. This is used to calculate their
     * geographic location.
     * @param deliveryService The Delivery Service being served. This is used to help geo-locate the
     * client according to blocking and fallback configuration.
     */
    fun geoSortSteeringResults(steeringResults: List<SteeringResult>?, clientIP: String?, deliveryService: DeliveryService?) {
        if (clientIP == null || clientIP.isEmpty()
                || steeringResults?.stream()?.allMatch { t: SteeringResult -> t.steeringTarget.geolocation == null } == true) {
            return
        }
        val clientLocation = getClientLocationByCoverageZoneOrGeo(clientIP, deliveryService)
        if (clientLocation != null) {
            Collections.sort(steeringResults, SteeringGeolocationComparator(clientLocation))
            Collections.sort(steeringResults, Comparator.comparingInt { s: SteeringResult -> s.steeringTarget.order }) // re-sort by order to preserve the ordering done by ConsistentHasher
        }
    }

    private fun consistentHashMultiDeliveryService(deliveryService: DeliveryService?, request: HTTPRequest): MutableList<SteeringResult>? {
        if (deliveryService == null) {
            return null
        }
        val steeringResults: MutableList<SteeringResult> = ArrayList()
        if (!isSteeringDeliveryService(deliveryService)) {
            steeringResults.add(SteeringResult(null, deliveryService))
            return steeringResults
        }
        val steering = steeringRegistry!![deliveryService.id]

        // Pattern based consistent hashing
        val pathToHash = buildPatternBasedHashString(deliveryService, request)
        val steeringTargets = consistentHasher.selectHashables(steering.targets, pathToHash)
        for (steeringTarget in steeringTargets) {
            val target = cacheRegister!!.getDeliveryService(steeringTarget.deliveryService)
            if (target != null) { // target might not be in CRConfig yet
                steeringResults.add(SteeringResult(steeringTarget, target))
            }
        }
        return steeringResults
    }

    /**
     * Chooses a [Cache] for a Steering Delivery Service target based on the Coverage Zone
     * File given a clients IP and request *path*.
     * @param ip The client's IP address
     * @param deliveryServiceId The "xml_id" of a Delivery Service being routed
     * @param requestPath The client's requested path - e.g.
     * `http://test.example.com/request/path`  `/request/path`
     * @return A cache object chosen to serve the client's request
     */
    fun consistentHashSteeringForCoverageZone(ip: String, deliveryServiceId: String, requestPath: String?): Cache? {
        val r = HTTPRequest()
        r.path = requestPath
        r.queryString = ""
        return consistentHashSteeringForCoverageZone(ip, deliveryServiceId, r)
    }

    /**
     * Chooses a [Cache] for a Steering Delivery Service target based on the Coverage Zone
     * File given a clients IP and request.
     * @param ip The client's IP address
     * @param deliveryServiceId The "xml_id" of a Delivery Service being routed
     * @param request The client's HTTP request
     * @return A cache object chosen to serve the client's request
     */
    private fun consistentHashSteeringForCoverageZone(ip: String, deliveryServiceId: String, request: HTTPRequest): Cache? {
        val deliveryService = consistentHashDeliveryService(deliveryServiceId, request)
        if (deliveryService == null) {
            LOGGER.error("Failed getting delivery service from cache register for id '$deliveryServiceId'")
            return null
        }
        val requestVersion = if (ip.contains(":")) IPVersions.IPV6ONLY else IPVersions.IPV4ONLY
        val coverageZoneCacheLocation = getCoverageZoneCacheLocation(ip, deliveryService, false, null, requestVersion)
        val caches = selectCachesByCZ(deliveryService, coverageZoneCacheLocation, requestVersion)
        if (caches == null || caches.isEmpty()) {
            return null
        }
        val pathToHash = buildPatternBasedHashString(deliveryService, request)
        return consistentHasher.selectHashable(caches, deliveryService.dispersion, pathToHash)
    }

    /**
     * Chooses a target Delivery Service of a given Delivery Service to service a given request path
     *
     * @param deliveryServiceId The "xml_id" of the Delivery Service being requested
     * @param requestPath The requested path - e.g.
     * `http://test.example.com/request/path`  `/request/path`
     * @return The chosen target Delivery Service, or null if one could not be determined.
     */
    fun consistentHashDeliveryService(deliveryServiceId: String?, requestPath: String?): DeliveryService? {
        val r = HTTPRequest()
        r.path = requestPath
        r.queryString = ""
        return consistentHashDeliveryService(deliveryServiceId, r)
    }

    /**
     * Chooses a target Delivery Service of a given Delivery Service to service a given request.
     *
     * @param deliveryServiceId The "xml_id" of the Delivery Service being requested
     * @param request The client's HTTP request
     * @return The chosen target Delivery Service, or null if one could not be determined.
     */
    private fun consistentHashDeliveryService(deliveryServiceId: String?, request: HTTPRequest): DeliveryService? {
        return consistentHashDeliveryService(cacheRegister!!.getDeliveryService(deliveryServiceId), request, "")
    }

    /**
     * Chooses a target Delivery Service of a given Delivery Service to service a given request and
     * [.XTC_STEERING_OPTION] value.
     *
     * @param deliveryService The DeliveryService being requested
     * @param request The client's HTTP request
     * @param xtcSteeringOption The value of the client's [.XTC_STEERING_OPTION] HTTP Header.
     * @return The chosen target Delivery Service, or null if one could not be determined.
     */
    fun consistentHashDeliveryService(deliveryService: DeliveryService?, request: HTTPRequest, xtcSteeringOption: String?): DeliveryService? {
        if (deliveryService == null) {
            return null
        }
        if (!isSteeringDeliveryService(deliveryService)) {
            return deliveryService
        }
        val steering = steeringRegistry!![deliveryService.id]
        if (xtcSteeringOption != null && xtcSteeringOption.isNotEmpty()) {
            return if (steering.hasTarget(xtcSteeringOption)) cacheRegister!!.getDeliveryService(xtcSteeringOption) else null
        }
        val bypassDeliveryServiceId = steering.getBypassDestination(request.path)
        if (bypassDeliveryServiceId != null && bypassDeliveryServiceId.isNotEmpty()) {
            val bypass = cacheRegister!!.getDeliveryService(bypassDeliveryServiceId)
            if (bypass != null) { // bypass DS target might not be in CRConfig yet. Until then, try existing targets
                return bypass
            }
        }

        // only select from targets in CRConfig
        val availableTargets = steering.targets.stream()
                .filter { target: SteeringTarget -> cacheRegister!!.getDeliveryService(target.deliveryService) != null }
                .collect(Collectors.toList())

        // Pattern based consistent hashing
        val pathToHash = buildPatternBasedHashString(deliveryService, request)
        val steeringTarget = consistentHasher.selectHashable(availableTargets, deliveryService.dispersion, pathToHash)

        // set target.consistentHashRegex from steering DS, if it is set
        val targetDeliveryService = cacheRegister!!.getDeliveryService(steeringTarget.deliveryService)
        if (deliveryService.consistentHashRegex != null && deliveryService.consistentHashRegex.isNotEmpty()) {
            targetDeliveryService.consistentHashRegex = deliveryService.consistentHashRegex
        }
        return targetDeliveryService
    }

    /**
     * Returns a list [Location]s sorted by distance from the client.
     * If the client's location could not be determined, then the list is
     * unsorted.
     *
     * @param locations the collection of Locations to order
     * @return the ordered list of locations
     */
    fun orderLocations(locations: List<Location>, clientLocation: Geolocation?): List<Location> {
        Collections.sort(locations, LocationComparator(clientLocation))
        return locations
    }

    private fun getClosestCacheLocation(cacheLocations: List<CacheLocation>, clientLocation: Geolocation?, deliveryService: DeliveryService, requestVersion: IPVersions?): CacheLocation? {
        if (clientLocation == null) {
            return null
        }
        val orderedLocations = orderLocations(cacheLocations, clientLocation)
        for (cacheLocation in orderedLocations) {
            cacheLocation as CacheLocation
            if (getSupportingCaches(cacheLocation.caches, deliveryService, requestVersion).isNotEmpty()) {
                return cacheLocation
            }
        }
        return null
    }

    /**
     * Selects a [Cache] from the [CacheLocation] provided.
     *
     * @param location
     * the caches that will considered
     * @param ds
     * the delivery service for the request
     * @return the selected cache or null if none can be found
     */
    private fun selectCaches(location: CacheLocation, ds: DeliveryService, requestVersion: IPVersions): MutableList<Cache?>? {
        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Trying location: " + location.id)
        }
        val caches = getSupportingCaches(location.caches, ds, requestVersion)
        if (caches.isEmpty()) {
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug("No online, supporting caches were found at location: "
                        + location.id)
            }
            return null
        }
        return caches
    }

    /**
     * Gets a DNS zone that contains a given name.
     * @param qname The DNS name that the returned zone will contain. This can include wildcards.
     * @param qtype The [Type](https://javadoc.io/doc/dnsjava/dnsjava/latest/org/xbill/DNS/Type.html)
     * of the record which will be returned for DNS queries for the returned zone.
     * @param clientAddress The IP address of the client making the DNS request. The zone that is
     * ultimately returned can depend on blocking configuration for a requested Delivery Service,
     * if the qname represents a Delivery Service routing name.
     * @param isDnssecRequest Tells whether or not the request was made using DNSSEC, which will
     * control whether or not the returned zone is signed.
     * @param builder Used to build a zone if one has not already been created containing qname.
     * @return A zone containing records of type qtype that contains qname. This can be null
     */
    fun getZone(qname: Name, qtype: Int, clientAddress: InetAddress, isDnssecRequest: Boolean, builder: DNSAccessRecord.Builder): Zone? {
        return zoneManager.getZone(qname, qtype, clientAddress, isDnssecRequest, builder)
    }

    private fun enforceGeoRedirect(track: Track, ds: DeliveryService, clientIp: String, queriedClientLocation: Geolocation?, requestVersion: IPVersions): MutableList<Cache?>? {
        val urlType = ds.geoRedirectUrlType
        track.setResult(ResultType.GEO_REDIRECT)
        if ("NOT_DS_URL" == urlType) {
            // redirect url not belongs to this DS, just redirect it
            return null
        }
        if ("DS_URL" != urlType) {
            LOGGER.error("invalid geo redirect url type '$urlType'")
            track.setResult(ResultType.MISS)
            track.setResultDetails(ResultDetails.GEO_NO_CACHE_FOUND)
            return null
        }
        var clientLocation = queriedClientLocation

        //redirect url belongs to this DS, will try return the caches
        if (clientLocation == null) {
            try {
                clientLocation = getLocation(clientIp, ds)
            } catch (e: GeolocationException) {
                LOGGER.warn("Failed getting geolocation for client ip " + clientIp + " and delivery service '" + ds.id + "'")
            }
        }
        if (clientLocation == null) {
            clientLocation = ds.missLocation
        }
        if (clientLocation == null) {
            LOGGER.error("cannot find a geo location for the client: $clientIp")
            // particular error was logged in ds.supportLocation
            track.setResult(ResultType.MISS)
            track.setResultDetails(ResultDetails.DS_CLIENT_GEO_UNSUPPORTED)
            return null
        }
        var caches: MutableList<Cache?>? = null
        try {
            caches = getCachesByGeo(ds, clientLocation, track, requestVersion)
        } catch (e: GeolocationException) {
            LOGGER.error("Failed getting caches by geolocation " + e.message)
        }
        if (caches == null) {
            LOGGER.warn(String.format("No Cache found by Geo in NGB redirect"))
            track.setResult(ResultType.MISS)
            track.setResultDetails(ResultDetails.GEO_NO_CACHE_FOUND)
        }
        return caches
    }

    @Throws(BeansException::class)
    fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext
    }

    fun configurationChanged() {
        if (applicationContext == null) {
            LOGGER.warn("Application Context not yet ready, skipping calling listeners of configuration change")
            return
        }
        val configurationListenerMap = applicationContext!!.getBeansOfType(ConfigurationListener::class.java)
        for (configurationListener in configurationListenerMap.values) {
            configurationListener.configurationChanged()
        }
    }

    fun setSteeringRegistry(steeringRegistry: SteeringRegistry?) {
        this.steeringRegistry = steeringRegistry
    }

    private fun getEdgeHTTPRoutingLimit(ds: DeliveryService?): Int {
        return if (ds != null && ds.maxDnsIps != 0 && ds.maxDnsIps != edgeHTTPRoutingLimit) {
            ds.maxDnsIps
        } else edgeHTTPRoutingLimit
    }
}
