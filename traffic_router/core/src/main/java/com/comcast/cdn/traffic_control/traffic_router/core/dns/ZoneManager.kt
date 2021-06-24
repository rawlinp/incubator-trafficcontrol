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
package com.comcast.cdn.traffic_control.traffic_router.core.dns

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.edge.InetRecord
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Node.IPVersions
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Resolver
import com.comcast.cdn.traffic_control.traffic_router.core.request.DNSRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.DNSRouteResult
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouterManager
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtils
import com.comcast.cdn.traffic_control.traffic_router.core.util.JsonUtilsException
import com.comcast.cdn.traffic_control.traffic_router.core.util.TrafficOpsUtils
import com.comcast.cdn.traffic_control.traffic_router.geolocation.GeolocationException

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheBuilderSpec
import com.google.common.cache.CacheLoader
import com.google.common.cache.CacheStats
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.apache.commons.io.IOUtils
import org.apache.log4j.Logger
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.ARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.DClass
import org.xbill.DNS.NSECRecord
import org.xbill.DNS.NSRecord
import org.xbill.DNS.Name
import org.xbill.DNS.RRSIGRecord
import org.xbill.DNS.RRset
import org.xbill.DNS.Record
import org.xbill.DNS.SOARecord
import org.xbill.DNS.TXTRecord
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type
import org.xbill.DNS.Zone

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class ZoneManager(tr: TrafficRouter, statTracker: StatTracker, trafficOpsUtils: TrafficOpsUtils?, trafficRouterManager: TrafficRouterManager) : Resolver() {
    /**
     * Gets trafficRouter.
     *
     * @return the trafficRouter
     */
    val trafficRouter: TrafficRouter
    val statTracker: StatTracker

    enum class ZoneCacheType {
        DYNAMIC, STATIC
    }

    fun rebuildZoneCache() {
        initZoneCache(trafficRouter)
    }

    private fun initSignatureManager(cacheRegister: CacheRegister, trafficOpsUtils: TrafficOpsUtils?, trafficRouterManager: TrafficRouterManager) {
        val sm = SignatureManager(this, cacheRegister, trafficOpsUtils, trafficRouterManager)
        signatureManager = sm
    }

    /**
     * Attempts to find a [Zone] that would contain the specified [Name].
     *
     * @param name
     * the Name to use to attempt to find the Zone
     * @return the Zone to use to resolve the specified Name
     */
    fun getZone(name: Name): Zone? {
        return getZone(name, 0)
    }

    /**
     * Attempts to find a [Zone] that would contain the specified [Name].
     *
     * @param name
     * the Name to use to attempt to find the Zone
     * @param qtype
     * the Type to use to control Zone ordering
     * @return the Zone to use to resolve the specified Name
     */
    fun getZone(name: Name, qtype: Int): Zone? {
        val zoneMap: Map<ZoneKey?, Zone?> = zoneCache!!.asMap()
        val sorted: List<ZoneKey?> = ArrayList(zoneMap.keys)
        var result: Zone? = null
        var target = name
        Collections.sort(sorted)
        if (qtype == Type.DS) {
            target = Name(name, 1) // DS records are in the parent zone, change target accordingly
        }
        for (key in sorted) {
            val zone = zoneMap[key]
            val origin = zone!!.origin
            if (target.subdomain(origin)) {
                result = zone
                break
            }
        }
        return result
    }

    /**
     * Creates a dynamic zone that serves a set of A and AAAA records for the specified [Name]
     * .
     *
     * @param staticZone
     * The Zone that would normally serve this request
     * @param builder
     * DNSAccessRecord.Builder access logging
     * @param request
     * DNSRequest representing the query
     * @return the new Zone to serve the request or null if the static Zone should be used
     */
    private fun createDynamicZone(staticZone: Zone?, builder: DNSAccessRecord.Builder, request: DNSRequest): Zone? {
        val track = StatTracker.getTrack()
        try {
            val result = trafficRouter.route(request, track)
            return if (result != null) {
                val dynamicZone = fillDynamicZone(dynamicZoneCache, staticZone, request, result)
                track.setResultCode(dynamicZone, request.name, request.queryType)
                dynamicZone
            } else {
                null
            }
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
        } finally {
            builder.resultType(track.result)
            builder.resultDetails(track.resultDetails)
            builder.resultLocation(track.resultLocation)
            statTracker.saveTrack(track)
        }
        return null
    }

    private fun lookup(qname: Name, zone: Zone, type: Int): List<InetRecord>? {
        val ipAddresses: MutableList<InetRecord> = ArrayList()
        val sr = zone.findRecords(qname, type)
        if (sr.isSuccessful) {
            val answers = sr.answers()
            for (answer in answers) {
                val it: Iterator<Record?> = answer.rrs() as Iterator<Record?>
                while (it.hasNext()) {
                    val r = it.next()
                    if (r is ARecord) {
                        val ar = r
                        ipAddresses.add(InetRecord(ar.address, ar.ttl))
                    } else if (r is AAAARecord) {
                        val ar = r
                        ipAddresses.add(InetRecord(ar.address, ar.ttl))
                    }
                }
            }
            return ipAddresses
        }
        return null
    }

    override fun resolve(fqdn: String): List<InetRecord>? {
        try {
            val name = Name(fqdn)
            val zone = getZone(name)
            if (zone == null) {
                LOGGER.error("No zone - Defaulting to system resolver: $fqdn")
                return super.resolve(fqdn)
            }
            return lookup(name, zone, Type.A)!!
        } catch (e: TextParseException) {
            LOGGER.warn("TextParseException from: $fqdn", e)
        }
        return null
    }

    @Throws(UnknownHostException::class)
    fun resolve(fqdn: String, address: String?, builder: DNSAccessRecord.Builder): List<InetRecord>? {
        try {
            val name = Name(fqdn)
            var zone = getZone(name)
            val addr = InetAddress.getByName(address)
            val qtype = if (addr is Inet6Address) Type.AAAA else Type.A
            val request = DNSRequest(zone, name, qtype)
            request.clientIP = addr.hostAddress
            request.hostname = name.relativize(Name.root).toString()
            request.isDnssec = true
            val dynamicZone = createDynamicZone(zone, builder, request)
            if (dynamicZone != null) {
                zone = dynamicZone
            }
            if (zone == null) {
                LOGGER.error("No zone - Defaulting to system resolver: $fqdn")
                return super.resolve(fqdn)
            }
            return lookup(name, zone, Type.A)
        } catch (e: TextParseException) {
            LOGGER.error("TextParseException from: $fqdn")
        }
        return null
    }

    fun getZone(qname: Name, qtype: Int, clientAddress: InetAddress, isDnssecRequest: Boolean, builder: DNSAccessRecord.Builder): Zone? {
        val zone = getZone(qname, qtype) ?: return null

        // all queries must be dynamic when edge DNS routing is enabled, as NS RRsets are used for the authority section and must be localized
        if (!trafficRouter.isEdgeDNSRouting) {
            val sr = zone.findRecords(qname, qtype)
            if (sr.isSuccessful) {
                return zone
            }
        }
        val request = DNSRequest(zone, qname, qtype)
        request.clientIP = clientAddress.hostAddress
        request.hostname = qname.relativize(Name.root).toString()
        request.isDnssec = isDnssecRequest
        val dynamicZone = createDynamicZone(zone, builder, request)
        return dynamicZone ?: zone
    }

    val staticCacheStats: CacheStats
        get() = zoneCache!!.stats()
    val dynamicCacheStats: CacheStats
        get() = dynamicZoneCache!!.stats()

    companion object {
        private val LOGGER = Logger.getLogger(ZoneManager::class.java)
        private var dynamicZoneCache: LoadingCache<ZoneKey?, Zone?>? = null
        private var zoneCache: LoadingCache<ZoneKey?, Zone?>? = null
        private var domainsToZoneKeys: ConcurrentMap<String, ZoneKey?> = ConcurrentHashMap()
        private var zoneMaintenanceExecutor: ScheduledExecutorService? = null
        private var zoneExecutor: ExecutorService? = null
        private const val DEFAULT_PRIMER_LIMIT = 500
        private const val IP = "ip"
        private const val IP6 = "ip6"
        var zoneDirectory: File? = null
        private var signatureManager: SignatureManager? = null
        @JvmStatic
        var topLevelDomain: Name? = null
            private set
        private const val AAAA = "AAAA"
        @JvmStatic
        fun destroy() {
            zoneMaintenanceExecutor!!.shutdownNow()
            zoneExecutor!!.shutdownNow()
            signatureManager!!.destroy()
        }

        @Throws(TextParseException::class)
        private fun initTopLevelDomain(data: CacheRegister) {
            var tld = JsonUtils.optString(data.config, "domain_name")
            if (!tld.endsWith(".")) {
                tld = "$tld."
            }
            topLevelDomain = Name(tld)
        }

        protected fun initZoneCache(tr: TrafficRouter) {
            synchronized(ZoneManager::class.java) {
                val cacheRegister = tr.cacheRegister
                val config = cacheRegister.config
                val poolSize = calcThreadPoolSize(config)
                val initExecutor = Executors.newFixedThreadPool(poolSize)
                val generationTasks: MutableList<Runnable> = ArrayList()
                val primingTasks: BlockingQueue<Runnable> = LinkedBlockingQueue()
                val ze = Executors.newFixedThreadPool(poolSize)
                val me = Executors.newScheduledThreadPool(2) // 2 threads, one for static, one for dynamic, threads to refresh zones
                val maintenanceInterval = JsonUtils.optInt(config, "zonemanager.cache.maintenance.interval", 300) // default 5 minutes
                val initTimeout = JsonUtils.optInt(config, "zonemanager.init.timeout", 10)
                val dzc = createZoneCache(ZoneCacheType.DYNAMIC, getDynamicZoneCacheSpec(config, poolSize))
                val zc = createZoneCache(ZoneCacheType.STATIC)
                val newDomainsToZoneKeys: ConcurrentMap<String, ZoneKey?> = ConcurrentHashMap()
                if (tr.isDnssecZoneDiffingEnabled) {
                    if (dynamicZoneCache == null || zoneCache == null) {
                        initZoneDirectory()
                    } else {
                        copyExistingDynamicZones(tr, dzc)
                    }
                } else {
                    initZoneDirectory()
                }
                try {
                    LOGGER.info("Generating zone data")
                    generateZones(tr, zc, dzc, generationTasks, primingTasks, newDomainsToZoneKeys)
                    initExecutor.invokeAll(generationTasks.stream().map { task: Runnable? -> Executors.callable(task) }.collect(Collectors.toList()))
                    LOGGER.info("Zone generation complete")
                    val primingStart = Instant.now()
                    val futures = initExecutor.invokeAll(primingTasks.stream().map { task: Runnable? -> Executors.callable(task) }.collect(Collectors.toList()), initTimeout.toLong(), TimeUnit.MINUTES)
                    val primingEnd = Instant.now()
                    if (futures.stream().anyMatch { obj: Future<Any> -> obj.isCancelled }) {
                        LOGGER.warn(String.format("Priming zone cache exceeded time limit of %d minute(s); continuing", initTimeout))
                    } else {
                        LOGGER.info(String.format("Priming zone cache completed in %s", Duration.between(primingStart, primingEnd).toString()))
                    }
                    me.scheduleWithFixedDelay(getMaintenanceRunnable(dzc, ZoneCacheType.DYNAMIC, maintenanceInterval), 0, maintenanceInterval.toLong(), TimeUnit.SECONDS)
                    me.scheduleWithFixedDelay(getMaintenanceRunnable(zc, ZoneCacheType.STATIC, maintenanceInterval), 0, maintenanceInterval.toLong(), TimeUnit.SECONDS)
                    val tze = zoneExecutor
                    val tme = zoneMaintenanceExecutor
                    val tzc = zoneCache
                    val tdzc = dynamicZoneCache
                    zoneExecutor = ze
                    zoneMaintenanceExecutor = me
                    dynamicZoneCache = dzc
                    zoneCache = zc
                    val oldZCSize = tzc?.size() ?: 0
                    val oldDCZSize = if (tzc == null) 0 else tdzc!!.size()
                    LOGGER.info("old static zone cache size: " + oldZCSize + ", new static zone cache size: " + zc.size() +
                            ", old dynamic zone cache size: " + oldDCZSize + ", new dynamic zone cache size: " + dzc.size())
                    domainsToZoneKeys = newDomainsToZoneKeys
                    tze?.shutdownNow()
                    tme?.shutdownNow()
                    tzc?.invalidateAll()
                    tdzc?.invalidateAll()
                    LOGGER.info("Initialization of zone data completed")
                } catch (ex: InterruptedException) {
                    LOGGER.warn(String.format("Initialization of zone data was interrupted, timeout of %d minute(s); continuing", initTimeout), ex)
                } catch (ex: IOException) {
                    LOGGER.fatal("Caught fatal exception while generating zone data!", ex)
                }
            }
        }

        private fun copyExistingDynamicZones(tr: TrafficRouter, dzc: LoadingCache<ZoneKey?, Zone?>) {
            val allZones = getAllDeliveryServiceDomains(tr)
            allZones[topLevelDomain!!.toString(true)] = null
            val dzcMap: Map<ZoneKey?, Zone?> = dynamicZoneCache!!.asMap()
            for (zoneKey in dzcMap.keys) {
                if (allZones.containsKey(zoneKey!!.name.toString(true))) {
                    dzc.put(zoneKey, dzcMap[zoneKey])
                } else {
                    LOGGER.info("domain for old zone " + zoneKey.name.toString(true) + " not found; will not copy it into new dynamic zone cache")
                }
            }
        }

        private fun calcThreadPoolSize(config: JsonNode): Int {
            var poolSize = 1
            val scale = JsonUtils.optDouble(config, "zonemanager.threadpool.scale", 0.75)
            val cores = Runtime.getRuntime().availableProcessors()
            if (cores > 2) {
                val s = Math.floor(cores.toDouble() * scale)
                if (s.toInt() > 1) {
                    poolSize = s.toInt()
                }
            }
            return poolSize
        }

        private fun getDynamicZoneCacheSpec(config: JsonNode, poolSize: Int): CacheBuilderSpec {
            val cacheSpec: MutableList<String> = ArrayList()
            cacheSpec.add("expireAfterAccess=" + JsonUtils.optString(config, "zonemanager.dynamic.response.expiration", "3600s")) // default to one hour
            cacheSpec.add("concurrencyLevel=" + JsonUtils.optString(config, "zonemanager.dynamic.concurrencylevel", poolSize.toString())) // default to pool size, 4 is the actual default
            cacheSpec.add("initialCapacity=" + JsonUtils.optInt(config, "zonemanager.dynamic.initialcapacity", 10000)) // set the initial capacity to avoid expensive resizing
            return CacheBuilderSpec.parse(cacheSpec.stream().collect(Collectors.joining(",")))
        }

        private fun getMaintenanceRunnable(cache: LoadingCache<ZoneKey?, Zone?>, type: ZoneCacheType, refreshInterval: Int): Runnable {
            return Runnable {
                LOGGER.info("starting maintenance on " + type.toString() + " zone cache: " + Integer.toHexString(cache.hashCode()) + ". Current size: " + cache.size())
                cache.cleanUp()
                for (zoneKey in cache.asMap().keys) {
                    try {
                        if (signatureManager!!.needsRefresh(type, zoneKey, refreshInterval)) {
                            cache.refresh(zoneKey)
                        }
                    } catch (ex: RuntimeException) {
                        LOGGER.fatal("RuntimeException caught on " + zoneKey!!.javaClass.simpleName + " for " + zoneKey.name, ex)
                    }
                }
                LOGGER.info("completed maintenance on " + type.toString() + " zone cache: " + Integer.toHexString(cache.hashCode()))
            }
        }

        private fun initZoneDirectory() {
            synchronized(LOGGER) {
                if (zoneDirectory!!.exists()) {
                    for (entry in zoneDirectory!!.list()) {
                        val zone = File(zoneDirectory!!.path, entry)
                        zone.delete()
                    }
                    val deleted = zoneDirectory!!.delete()
                    if (!deleted) {
                        LOGGER.warn("Unable to delete " + zoneDirectory)
                    }
                }
                zoneDirectory!!.mkdir()
            }
        }

        @Throws(IOException::class)
        private fun writeZone(zone: Zone) {
            synchronized(LOGGER) {
                if (!zoneDirectory!!.exists() && !zoneDirectory!!.mkdirs()) {
                    LOGGER.error(zoneDirectory!!.absolutePath + " directory does not exist and cannot be created!")
                }
                val zoneFile = File(zoneDirectory, zone.origin.toString())
                val w = FileWriter(zoneFile)
                LOGGER.info("writing: " + zoneFile.absolutePath)
                IOUtils.write(zone.toMasterFile(), w)
                w.flush()
                w.close()
            }
        }

        private fun createZoneCache(cacheType: ZoneCacheType, spec: CacheBuilderSpec = CacheBuilderSpec.parse("")): LoadingCache<ZoneKey?, Zone?> {
            val removalListener: RemovalListener<ZoneKey, Zone> = RemovalListener { removal -> LOGGER.debug(cacheType.toString() + " " + removal.key!!.javaClass.simpleName + " " + removal.key!!.name + " evicted from cache: " + removal.cause) }
            return CacheBuilder.from(spec).recordStats().removalListener(removalListener).build(
                    object : CacheLoader<ZoneKey?, Zone?>() {
                        val writeZone = cacheType == ZoneCacheType.STATIC
                        @Throws(IOException::class, GeneralSecurityException::class)
                        override fun load(zoneKey: ZoneKey?): Zone? {
                            if (zoneKey == null) {
                                return null
                            }
                            LOGGER.debug("loading " + cacheType + " " + zoneKey.javaClass.simpleName + " " + zoneKey.name)
                            return loadZone(zoneKey, writeZone)
                        }

                        @Throws(IOException::class, GeneralSecurityException::class)
                        override fun reload(zoneKey: ZoneKey?, prevZone: Zone?): ListenableFuture<Zone?>? {
                            if (zoneKey == null) {
                                return null
                            }
                            val zoneTask = ListenableFutureTask.create { loadZone(zoneKey, writeZone) }
                            zoneExecutor!!.execute(zoneTask)
                            return zoneTask
                        }
                    }
            )
        }

        @Throws(IOException::class, GeneralSecurityException::class)
        fun loadZone(zoneKey: ZoneKey, writeZone: Boolean): Zone {
            LOGGER.debug("Attempting to load " + zoneKey.name)
            val name = zoneKey.name
            var records = zoneKey.getRecords()
            zoneKey.updateTimestamp()
            if (zoneKey is SignedZoneKey) {
                records = signatureManager!!.signZone(name, records, zoneKey)
            }
            val zone = Zone(name, records.toTypedArray())
            if (writeZone) {
                writeZone(zone)
            }
            return zone
        }

        private fun getAllDeliveryServiceDomains(tr: TrafficRouter): MutableMap<String, DeliveryService?> {
            val data = tr.cacheRegister
            val dsMap: MutableMap<String, DeliveryService?> = HashMap()
            val tld = topLevelDomain!!.toString(true) // Name.toString(true) - omit the trailing dot
            for (ds in data.deliveryServices.values) {
                var domain = ds.domain ?: continue
                if (domain.endsWith("+")) {
                    domain = domain.replace("\\+\\z".toRegex(), ".") + tld
                }
                if (domain.endsWith(tld)) {
                    dsMap[domain] = ds
                }
            }
            return dsMap
        }

        @Throws(IOException::class)
        private fun generateZones(tr: TrafficRouter, zc: LoadingCache<ZoneKey?, Zone?>, dzc: LoadingCache<ZoneKey?, Zone?>,
                                  generationTasks: MutableList<Runnable>, primingTasks: BlockingQueue<Runnable>,
                                  newDomainsToZoneKeys: ConcurrentMap<String, ZoneKey?>) {
            val dsMap = getAllDeliveryServiceDomains(tr)
            val data = tr.cacheRegister
            val zoneMap: MutableMap<String, MutableList<Record>> = HashMap()
            val superDomains = populateZoneMap(zoneMap, dsMap, data)
            val superRecords = fillZones(zoneMap, dsMap, tr, zc, dzc, generationTasks, primingTasks, newDomainsToZoneKeys)
            val upstreamRecords = fillZones(superDomains, dsMap, tr, superRecords, zc, dzc, generationTasks, primingTasks, newDomainsToZoneKeys)
            for (record in upstreamRecords) {
                if (record.type == Type.DS) {
                    LOGGER.info("Publish this DS record in the parent zone: $record")
                }
            }
        }

        @Throws(IOException::class)
        private fun fillZones(zoneMap: Map<String, MutableList<Record>>, dsMap: Map<String, DeliveryService?>, tr: TrafficRouter, zc: LoadingCache<ZoneKey?, Zone?>, dzc: LoadingCache<ZoneKey?, Zone?>, generationTasks: MutableList<Runnable>, primingTasks: BlockingQueue<Runnable>, newDomainsToZoneKeys: ConcurrentMap<String, ZoneKey?>): List<Record> {
            return fillZones(zoneMap, dsMap, tr, null, zc, dzc, generationTasks, primingTasks, newDomainsToZoneKeys)
        }

        @Throws(IOException::class)
        private fun fillZones(zoneMap: Map<String, MutableList<Record>>, dsMap: Map<String, DeliveryService?>, tr: TrafficRouter, superRecords: List<Record>?, zc: LoadingCache<ZoneKey?, Zone?>, dzc: LoadingCache<ZoneKey?, Zone?>, generationTasks: MutableList<Runnable>, primingTasks: BlockingQueue<Runnable>, newDomainsToZoneKeys: ConcurrentMap<String, ZoneKey?>): List<Record> {
            val hostname = InetAddress.getLocalHost().hostName.replace("\\..*".toRegex(), "")
            val records: MutableList<Record> = ArrayList()
            for (domain in zoneMap.keys) {
                if (superRecords != null && !superRecords.isEmpty()) {
                    zoneMap[domain]!!.addAll(superRecords)
                }
                records.addAll(createZone(domain, zoneMap, dsMap, tr, zc, dzc, generationTasks, primingTasks, hostname, newDomainsToZoneKeys))
            }
            return records
        }

        @Throws(IOException::class)
        private fun createZone(domain: String, zoneMap: Map<String, MutableList<Record>>, dsMap: Map<String, DeliveryService?>,
                               tr: TrafficRouter, zc: LoadingCache<ZoneKey?, Zone?>, dzc: LoadingCache<ZoneKey?, Zone?>, generationTasks: MutableList<Runnable>,
                               primingTasks: BlockingQueue<Runnable>, hostname: String, newDomainsToZoneKeys: ConcurrentMap<String, ZoneKey?>): List<Record> {
            val ds = dsMap[domain]
            val data = tr.cacheRegister
            val trafficRouters = data.trafficRouters
            val config = data.config
            var ttl: JsonNode? = null
            var soa: JsonNode? = null
            if (ds != null) {
                ttl = ds.ttls
                soa = ds.soa
            } else {
                ttl = config["ttls"]
                soa = config["soa"]
            }
            val name = newName(domain)
            val list = zoneMap[domain]!!
            val admin = newName(ZoneUtils.getAdminString(soa, "admin", "traffic_ops", domain))
            list.add(SOARecord(name, DClass.IN,
                    ZoneUtils.getLong(ttl, "SOA", 86400), getGlueName(ds, trafficRouters[hostname], name, hostname), admin,
                    ZoneUtils.getLong(soa, "serial", ZoneUtils.getSerial(data.stats)),
                    ZoneUtils.getLong(soa, "refresh", 28800),
                    ZoneUtils.getLong(soa, "retry", 7200),
                    ZoneUtils.getLong(soa, "expire", 604800),
                    ZoneUtils.getLong(soa, "minimum", 60)))
            addTrafficRouters(list, trafficRouters, name, ttl, domain, ds, tr)
            addStaticDnsEntries(list, ds, domain)
            val records: MutableList<Record> = ArrayList()
            val maxTTL = ZoneUtils.getMaximumTTL(list)
            try {
                records.addAll(signatureManager!!.generateDSRecords(name, maxTTL))
                list.addAll(signatureManager!!.generateDNSKEYRecords(name, maxTTL))
            } catch (ex: NoSuchAlgorithmException) {
                LOGGER.fatal("Unable to create zone: " + ex.message, ex)
            }
            primeZoneCache(domain, name, list, tr, zc, dzc, generationTasks, primingTasks, ds, newDomainsToZoneKeys)
            return records
        }

        private fun primeZoneCache(domain: String, name: Name, list: List<Record>, tr: TrafficRouter,
                                   zc: LoadingCache<ZoneKey?, Zone?>, dzc: LoadingCache<ZoneKey?, Zone?>, generationTasks: MutableList<Runnable>,
                                   primingTasks: BlockingQueue<Runnable>, ds: DeliveryService?, newDomainsToZoneKeys: ConcurrentMap<String, ZoneKey?>) {
            generationTasks.add(Runnable add@{
                try {
                    val newZoneKey = signatureManager!!.generateZoneKey(name, list)
                    if (tr.isDnssecZoneDiffingEnabled && domainsToZoneKeys.containsKey(domain)) {
                        val oldZoneKey = domainsToZoneKeys[domain]
                        if (zonesAreEqual(newZoneKey.getRecords(), oldZoneKey!!.getRecords())) {
                            val oldZone = zoneCache!!.getIfPresent(oldZoneKey)
                            if (oldZone != null) {
                                LOGGER.info("found matching ZoneKey for $domain - copying from current Zone cache into new Zone cache - no re-signing necessary")
                                zc.put(oldZoneKey, oldZone)
                                newDomainsToZoneKeys[domain] = oldZoneKey
                                return@add
                            }
                            LOGGER.warn("found matching ZoneKey for $domain but the Zone was not found in the Zone cache")
                        } else {
                            LOGGER.info("new zone for $domain is not equal to the old zone - re-signing necessary")
                        }
                    }
                    val zone = zc[newZoneKey] // cause the zone to be loaded into the new cache
                    if (tr.isDnssecZoneDiffingEnabled) {
                        newDomainsToZoneKeys[domain] = newZoneKey
                    }
                    val data = tr.cacheRegister
                    val config = data.config
                    val primeDynCache = JsonUtils.optBoolean(config, "dynamic.cache.primer.enabled", true)
                    if (!primeDynCache || ds == null || !ds.isDns && !tr.isEdgeHTTPRouting) {
                        return@add
                    }
                    primingTasks.add(Runnable {
                        try {
                            // prime the dynamic zone cache
                            if (ds.isDns) {
                                primeDNSDeliveryServices(domain, name, tr, dzc, zone, ds, data)
                            } else if (!ds.isDns && tr.isEdgeHTTPRouting) {
                                primeHTTPDeliveryServices(domain, tr, dzc, zone, ds, data)
                            }
                        } catch (ex: TextParseException) {
                            LOGGER.fatal("Unable to prime dynamic zone $domain", ex)
                        }
                    })
                } catch (ex: ExecutionException) {
                    LOGGER.fatal("Unable to load zone into cache: " + ex.message, ex)
                }
            })
        }

        @Throws(TextParseException::class)
        private fun primeHTTPDeliveryServices(domain: String, tr: TrafficRouter, dzc: LoadingCache<ZoneKey?, Zone?>,
                                              zone: Zone?, ds: DeliveryService, data: CacheRegister) {
            val edgeName = newName(ds.routingName, domain)
            LOGGER.info("Priming $edgeName")
            val request = DNSRequest(zone, edgeName, Type.A)
            request.isDnssec = signatureManager!!.isDnssecEnabled
            request.hostname = edgeName.toString(true) // Name.toString(true) - omit the trailing dot

            // prime the miss case first
            try {
                val result = DNSRouteResult()
                result.addresses = tr.selectTrafficRoutersMiss(request.zoneName, ds)
                fillDynamicZone(dzc, zone, request, result)
            } catch (ex: GeolocationException) {
                LOGGER.warn(ex, ex)
            }

            // prime answers for each of our edge locations
            for (trLocation in data.edgeTrafficRouterLocations) {
                try {
                    val result = DNSRouteResult()
                    result.addresses = tr.selectTrafficRoutersLocalized(trLocation.geolocation, request.zoneName, ds)
                    fillDynamicZone(dzc, zone, request, result)
                } catch (ex: GeolocationException) {
                    LOGGER.warn(ex, ex)
                }
            }
        }

        @Throws(TextParseException::class)
        private fun primeDNSDeliveryServices(domain: String, name: Name, tr: TrafficRouter, dzc: LoadingCache<ZoneKey?, Zone?>,
                                             zone: Zone?, ds: DeliveryService, data: CacheRegister) {
            val edgeName = newName(ds.routingName, domain)
            val config = data.config
            val primerLimit = JsonUtils.optInt(config, "dynamic.cache.primer.limit", DEFAULT_PRIMER_LIMIT)
            LOGGER.info("Priming $edgeName")
            val request = DNSRequest(zone, name, Type.A)
            request.isDnssec = signatureManager!!.isDnssecEnabled
            request.hostname = edgeName.toString(true) // Name.toString(true) - omit the trailing dot
            for (cacheLocation in data.cacheLocations) {
                val caches = tr.selectCachesByCZ(ds, cacheLocation, IPVersions.ANY) ?: continue

                // calculate number of permutations if maxDnsIpsForLocation > 0 and we're not using consistent DNS routing
                var p = 1
                if (ds.isDns && ds.maxDnsIps > 0 && !tr.isConsistentDNSRouting && caches.size > ds.maxDnsIps) {
                    for (c in caches.size downTo caches.size - ds.maxDnsIps + 1) {
                        p *= c
                    }
                }
                val pset: MutableSet<List<InetRecord>> = HashSet()
                for (i in 0 until primerLimit) {
                    val records = tr.inetRecordsFromCaches(ds, caches as MutableList<Cache?>, request)
                    val result = DNSRouteResult()
                    result.addresses = records
                    if (!pset.contains(records)) {
                        if (!tr.isEdgeDNSRouting) {
                            fillDynamicZone(dzc, zone, request, result)
                        } else {
                            try {
                                val hitResult = DNSRouteResult()
                                val hitRecords = tr.selectTrafficRoutersLocalized(cacheLocation.geolocation, request.zoneName, ds).toMutableList()
                                hitRecords.addAll(records)
                                hitResult.addresses = hitRecords
                                fillDynamicZone(dzc, zone, request, hitResult)
                            } catch (ex: GeolocationException) {
                                LOGGER.warn(ex, ex)
                            }
                        }
                        pset.add(records)
                    }
                    LOGGER.debug("Primed " + ds.id + " @ " + cacheLocation.id + "; permutation " + pset.size + "/" + p)
                    if (pset.size == p) {
                        break
                    }
                }
            }
        }

        // Check if the zones are equal except for the SOA record serial number, NSEC, or RRSIG records
        fun zonesAreEqual(newRecords: List<Record>, oldRecords: List<Record>): Boolean {
            val oldRecordsCopy: MutableList<Record> = oldRecords
                    .filter { it !is NSECRecord && it !is RRSIGRecord }
                    .toMutableList()
            val newRecordsCopy: MutableList<Record> = newRecords
                    .filter { it !is NSECRecord && it !is RRSIGRecord }
                    .toMutableList()
            if (oldRecordsCopy.size != newRecordsCopy.size) {
                return false
            }
            oldRecordsCopy.sort()
            newRecordsCopy.sort()
            for (i in newRecordsCopy.indices) {
                val newRec = newRecordsCopy[i]
                val oldRec = oldRecordsCopy[i]
                if (newRec is SOARecord && oldRec is SOARecord) {
                    val newSOA = newRec
                    val oldSOA = oldRec
                    // cmpSOA is a copy of newSOA except with the serial of oldSOA
                    val cmpSOA = SOARecord(newSOA.name, newSOA.dClass, newSOA.ttl,
                            newSOA.host, newSOA.admin, oldSOA.serial, newSOA.refresh,
                            newSOA.retry, newSOA.expire, newSOA.minimum)
                    if (oldSOA == cmpSOA && oldSOA.ttl == cmpSOA.ttl) {
                        continue
                    }
                    return false
                }
                if (newRec == oldRec && newRec.ttl == oldRec.ttl) {
                    continue
                }
                return false
            }
            return true
        }

        @Throws(TextParseException::class, UnknownHostException::class)
        private fun addStaticDnsEntries(list: MutableList<Record>, ds: DeliveryService?, domain: String) {
            if (ds != null && ds.staticDnsEntries != null) {
                val entryList = ds.staticDnsEntries
                for (staticEntry in entryList) {
                    try {
                        val type = JsonUtils.getString(staticEntry, "type").toUpperCase()
                        val jsName = JsonUtils.getString(staticEntry, "name")
                        val value = JsonUtils.getString(staticEntry, "value")
                        val name = newName(jsName, domain)
                        var ttl = JsonUtils.optInt(staticEntry, "ttl").toLong()
                        if (ttl == 0L) {
                            ttl = ZoneUtils.getLong(ds.ttls, type, 60)
                        }
                        when (type) {
                            "A" -> list.add(ARecord(name, DClass.IN, ttl, InetAddress.getByName(value)))
                            "AAAA" -> list.add(AAAARecord(name, DClass.IN, ttl, InetAddress.getByName(value)))
                            "CNAME" -> list.add(CNAMERecord(name, DClass.IN, ttl, Name(value)))
                            "TXT" -> list.add(TXTRecord(name, DClass.IN, ttl, value))
                        }
                    } catch (ex: JsonUtilsException) {
                        LOGGER.error(ex)
                    }
                }
            }
        }

        @Throws(TextParseException::class, UnknownHostException::class)
        private fun addTrafficRouters(list: MutableList<Record>, trafficRouters: JsonNode, name: Name,
                                      ttl: JsonNode?, domain: String, ds: DeliveryService?, tr: TrafficRouter) {
            val ip6RoutingEnabled = if (ds == null || ds != null && ds.isIp6RoutingEnabled) true else false
            val keyIter = trafficRouters.fieldNames()
            while (keyIter.hasNext()) {
                val key = keyIter.next()
                val trJo = trafficRouters[key]
                if (!trJo.has("status") || "OFFLINE" == trJo["status"].asText() || "ADMIN_DOWN" == trJo["status"].asText()) {
                    continue
                }
                val trName = newName(key, domain)

                // NSRecords will be replaced later if tr.isEdgeDNSRouting() is true; we need these to allow stub zones to be signed, etc
                list.add(NSRecord(name, DClass.IN, ZoneUtils.getLong(ttl, "NS", 60), getGlueName(ds, trJo, name, key)))
                list.add(ARecord(trName,
                        DClass.IN, ZoneUtils.getLong(ttl, "A", 60),
                        InetAddress.getByName(JsonUtils.optString(trJo, IP))))
                var ip6 = trJo["ip6"].asText()
                if (ip6 != null && !ip6.isEmpty() && ip6RoutingEnabled) {
                    ip6 = ip6.replace("/.*".toRegex(), "")
                    list.add(AAAARecord(trName,
                            DClass.IN,
                            ZoneUtils.getLong(ttl, AAAA, 60),
                            Inet6Address.getByName(ip6)))
                }

                // only add static routing name entries for HTTP DSs if necessary
                if (ds != null && !ds.isDns && !tr.isEdgeHTTPRouting) {
                    addHttpRoutingRecords(list, ds.routingName, domain, trJo, ttl, ip6RoutingEnabled)
                }
            }
        }

        @Throws(TextParseException::class, UnknownHostException::class)
        private fun addHttpRoutingRecords(list: MutableList<Record>, routingName: String, domain: String, trJo: JsonNode, ttl: JsonNode?, addTrafficRoutersAAAA: Boolean) {
            val trName = newName(routingName, domain)
            list.add(ARecord(trName,
                    DClass.IN,
                    ZoneUtils.getLong(ttl, "A", 60),
                    InetAddress.getByName(JsonUtils.optString(trJo, IP))))
            var ip6 = JsonUtils.optString(trJo, IP6)
            if (addTrafficRoutersAAAA && ip6 != null && !ip6.isEmpty()) {
                ip6 = ip6.replace("/.*".toRegex(), "")
                list.add(AAAARecord(trName,
                        DClass.IN,
                        ZoneUtils.getLong(ttl, AAAA, 60),
                        Inet6Address.getByName(ip6)))
            }
        }

        @Throws(TextParseException::class)
        private fun newName(hostname: String, domain: String): Name {
            return newName("$hostname.$domain")
        }

        @Throws(TextParseException::class)
        private fun newName(fqdn: String): Name {
            return if (fqdn.endsWith(".")) {
                Name(fqdn)
            } else {
                Name("$fqdn.")
            }
        }

        @Throws(TextParseException::class)
        private fun getGlueName(ds: DeliveryService?, trJo: JsonNode?, name: Name, trName: String): Name {
            return if (ds == null && trJo != null && trJo.has("fqdn") && trJo["fqdn"].textValue() != null) {
                newName(trJo["fqdn"].textValue())
            } else {
                val superDomain = Name(Name(name.toString(true)), 1)
                newName(trName, superDomain.toString())
            }
        }

        @Throws(IOException::class)
        private fun populateZoneMap(zoneMap: MutableMap<String, MutableList<Record>>,
                                    dsMap: MutableMap<String, DeliveryService?>, data: CacheRegister): Map<String, MutableList<Record>> {
            val superDomains: MutableMap<String, MutableList<Record>> = HashMap()
            for (domain in dsMap.keys) {
                zoneMap[domain] = ArrayList()
            }
            for (c in data.cacheMap.values) {
                for (dsr in c.deliveryServices) {
                    val ds = data.getDeliveryService(dsr.deliveryServiceId)
                    if (ds == null) {
                        LOGGER.warn("Content server " + c.fqdn + " has delivery service " + dsr.deliveryServiceId + " assigned, but the delivery service was not found. Skipping.")
                        continue
                    }
                    val fqdn = dsr.fqdn
                    val parts = fqdn.split(Regex("\\."), 2).toTypedArray()
                    val host = parts[0]
                    val domain = parts[1]
                    dsMap[domain] = ds
                    val zholder = zoneMap.computeIfAbsent(domain) { k: String? -> ArrayList() }
                    val superdomain = domain.split(Regex("\\."), 2).toTypedArray()[1]
                    if (!superDomains.containsKey(superdomain)) {
                        superDomains[superdomain] = ArrayList()
                    }
                    if (ds.isDns && host.equals(ds.routingName, ignoreCase = true)) {
                        continue
                    }
                    try {
                        val name = newName(fqdn)
                        val ttl = ds.ttls
                        val ip4 = c.ip4
                        if (ip4 != null) {
                            try {
                                zholder.add(ARecord(name, DClass.IN, ZoneUtils.getLong(ttl, "A", 60), ip4))
                            } catch (e: IllegalArgumentException) {
                                LOGGER.warn("$e : $ip4", e)
                            }
                        }
                        val ip6 = c.ip6
                        if (ip6 != null && ds.isIp6RoutingEnabled) {
                            try {
                                zholder.add(AAAARecord(name, DClass.IN, ZoneUtils.getLong(ttl, AAAA, 60), ip6))
                            } catch (e: IllegalArgumentException) {
                                LOGGER.warn("$e : $ip6", e)
                            }
                        }
                    } catch (e: TextParseException) {
                        LOGGER.error("Caught fatal exception while generating zone data for $fqdn!", e)
                    }
                }
            }
            return superDomains
        }

        private fun fillDynamicZone(dzc: LoadingCache<ZoneKey?, Zone?>?, staticZone: Zone?, request: DNSRequest, result: DNSRouteResult?): Zone? {
            if (result == null || result.addresses == null) {
                return null
            }
            try {
                var nsSeen = false
                val records: MutableList<Record?> = ArrayList()
                for (address in result.addresses) {
                    val ds = result.deliveryService
                    var name = request.name
                    if (address.type == Type.NS) {
                        name = staticZone!!.origin
                    } else if (ds != null && (address.type == Type.A || address.type == Type.AAAA)) {
                        val routingName = ds.routingName
                        name = Name(routingName, staticZone!!.origin) // routingname.ds.cdn.tld
                    }
                    val record = createRecord(name, address)
                    if (record != null) {
                        records.add(record)
                    }
                    if (record is NSRecord) {
                        nsSeen = true
                    }
                }

                // populate the dynamic zone with any static entries that aren't NS records or routing names
                val it: Iterator<RRset?> = staticZone!!.iterator() as Iterator<RRset?>
                while (it.hasNext()) {
                    val rrset = it.next()
                    val rit: Iterator<Record?> = rrset!!.rrs() as Iterator<Record?>
                    while (rit.hasNext()) {
                        val r = rit.next()
                        if (r is NSRecord) { // NSRecords are handled below
                            continue
                        }
                        records.add(r)
                    }
                }
                if (!records.isEmpty()) {
                    if (!nsSeen) {
                        records.addAll(createZoneNSRecords(staticZone))
                    }
                    try {
                        val zoneKey = signatureManager!!.generateDynamicZoneKey(staticZone.origin, records, request.isDnssec)
                        return dzc!![zoneKey]
                    } catch (e: ExecutionException) {
                        LOGGER.error(e, e)
                    }
                    return Zone(staticZone.origin, records.toTypedArray())
                }
            } catch (e: IOException) {
                LOGGER.error(e.message, e)
            }
            return null
        }

        @Throws(TextParseException::class)
        private fun createRecord(name: Name, address: InetRecord): Record? {
            var record: Record? = null
            if (address.isAlias) {
                record = CNAMERecord(name, DClass.IN, address.ttl, newName(address.alias))
            } else if (address.type == Type.NS) {
                val tld = topLevelDomain
                var target = address.target

                // fix up target to be TR host name plus top level domain
                if (name.subdomain(tld) && name != tld) {
                    target = String.format("%s.%s", target.split(Regex("\\."), 2).toTypedArray()[0], tld.toString())
                }
                record = NSRecord(name, DClass.IN, address.ttl, newName(target))
            } else if (address.isInet4) { // address instanceof Inet4Address
                record = ARecord(name, DClass.IN, address.ttl, address.address)
            } else if (address.isInet6) {
                record = AAAARecord(name, DClass.IN, address.ttl, address.address)
            }
            return record
        }

        @Throws(IOException::class)
        private fun createZoneNSRecords(staticZone: Zone?): List<Record?> {
            val records: MutableList<Record?> = ArrayList()
            val ns: Iterator<Record?> = staticZone!!.ns.rrs() as Iterator<Record?>
            while (ns.hasNext()) {
                records.add(ns.next())
            }
            return records
        }

    }

    init {
        initTopLevelDomain(tr.cacheRegister)
        initSignatureManager(tr.cacheRegister, trafficOpsUtils, trafficRouterManager)
        initZoneCache(tr)
        trafficRouter = tr
        this.statTracker = statTracker
    }
}