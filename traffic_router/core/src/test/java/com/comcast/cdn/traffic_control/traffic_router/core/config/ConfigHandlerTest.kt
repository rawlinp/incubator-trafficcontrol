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
package com.comcast.cdn.traffic_control.traffic_router.core.config

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryServiceMatcher
import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation.LocalizationMethod
import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheRegister
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.StatTracker
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.reflect.Whitebox
import java.util.*

class ConfigHandlerTest {
    private var handler: ConfigHandler? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        handler = Mockito.mock(ConfigHandler::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun itTestRelativeUrl() {
        val redirectUrl = "relative/url"
        val dsId = "relative-url"
        val urlType = arrayOf("")
        val typeUrl = arrayOf("")
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        val ds = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(dsId)
        Mockito.`when`(ds.geoRedirectUrl).thenReturn(redirectUrl)
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            typeUrl[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectFile = Matchers.anyString()
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            urlType[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectUrlType = Matchers.anyString()
        dsMap[dsId] = ds
        val register = PowerMockito.mock(CacheRegister::class.java)
        Whitebox.invokeMethod<Any>(handler, "initGeoFailedRedirect", dsMap, register)
        MatcherAssert.assertThat(urlType[0], org.hamcrest.Matchers.equalTo("DS_URL"))
        MatcherAssert.assertThat(typeUrl[0], org.hamcrest.Matchers.equalTo(""))
    }

    @Test
    @Throws(Exception::class)
    fun itTestRelativeUrlNegative() {
        val redirectUrl = "://invalid"
        val dsId = "relative-url"
        val urlType = arrayOf("")
        val typeUrl = arrayOf("")
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        val ds = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(dsId)
        Mockito.`when`(ds.geoRedirectUrl).thenReturn(redirectUrl)
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            typeUrl[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectFile = Matchers.anyString()
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            urlType[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectUrlType = Matchers.anyString()
        dsMap[dsId] = ds
        val register = PowerMockito.mock(CacheRegister::class.java)
        Whitebox.invokeMethod<Any>(handler, "initGeoFailedRedirect", dsMap, register)
        MatcherAssert.assertThat(urlType[0], org.hamcrest.Matchers.equalTo(""))
        MatcherAssert.assertThat(typeUrl[0], org.hamcrest.Matchers.equalTo(""))
    }

    @Test
    @Throws(Exception::class)
    fun itTestNoSuchDsUrl() {
        val path = "/ds/url"
        val redirectUrl = "http://test.com$path"
        val dsId = "relative-url"
        val urlType = arrayOf("")
        val typeUrl = arrayOf("")
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        val ds = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(dsId)
        Mockito.`when`(ds.geoRedirectUrl).thenReturn(redirectUrl)
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            typeUrl[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectFile = Matchers.anyString()
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            urlType[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectUrlType = Matchers.anyString()
        dsMap[dsId] = ds
        val register = PowerMockito.mock(CacheRegister::class.java)
        Mockito.`when`(
            register.getDeliveryService(
                Matchers.any(
                    HTTPRequest::class.java
                )
            )
        ).thenReturn(null)
        Whitebox.invokeMethod<Any>(handler, "initGeoFailedRedirect", dsMap, register)
        MatcherAssert.assertThat(urlType[0], org.hamcrest.Matchers.equalTo("NOT_DS_URL"))
        MatcherAssert.assertThat(typeUrl[0], org.hamcrest.Matchers.equalTo(path))
    }

    @Test
    @Throws(Exception::class)
    fun itTestNotThisDsUrl() {
        val path = "/ds/url"
        val redirectUrl = "http://test.com$path"
        val dsId = "relative-ds"
        val anotherId = "another-ds"
        val urlType = arrayOf("")
        val typeUrl = arrayOf("")
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        val ds = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(dsId)
        Mockito.`when`(ds.geoRedirectUrl).thenReturn(redirectUrl)
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            typeUrl[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectFile = Matchers.anyString()
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            urlType[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectUrlType = Matchers.anyString()
        dsMap[dsId] = ds
        val anotherDs = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(anotherId)
        val register = PowerMockito.mock(CacheRegister::class.java)
        Mockito.`when`(
            register.getDeliveryService(
                Matchers.any(
                    HTTPRequest::class.java
                )
            )
        ).thenReturn(anotherDs)
        Whitebox.invokeMethod<Any>(handler, "initGeoFailedRedirect", dsMap, register)
        MatcherAssert.assertThat(urlType[0], org.hamcrest.Matchers.equalTo("NOT_DS_URL"))
        MatcherAssert.assertThat(typeUrl[0], org.hamcrest.Matchers.equalTo(path))
    }

    @Test
    @Throws(Exception::class)
    fun itTestThisDsUrl() {
        val path = "/ds/url"
        val redirectUrl = "http://test.com$path"
        val dsId = "relative-ds"
        val urlType = arrayOf("")
        val typeUrl = arrayOf("")
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        val ds = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(dsId)
        Mockito.`when`(ds.geoRedirectUrl).thenReturn(redirectUrl)
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            typeUrl[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectFile = Matchers.anyString()
        Mockito.doAnswer { invocation ->
            val args = invocation.arguments
            urlType[0] = args[0] as String
            null
        }.`when`(ds).geoRedirectUrlType = Matchers.anyString()
        dsMap[dsId] = ds
        val register = PowerMockito.mock(CacheRegister::class.java)
        Mockito.`when`(
            register.getDeliveryService(
                Matchers.any(
                    HTTPRequest::class.java
                )
            )
        ).thenReturn(ds)
        Whitebox.invokeMethod<Any>(handler, "initGeoFailedRedirect", dsMap, register)
        MatcherAssert.assertThat(urlType[0], org.hamcrest.Matchers.equalTo("DS_URL"))
        MatcherAssert.assertThat(typeUrl[0], org.hamcrest.Matchers.equalTo(path))
    }

    @Test
    @Throws(Exception::class)
    fun itParsesTheTopologiesConfig() {
        /* Make the CacheLocation, add a Cache, and add the CacheLocation to the CacheRegister */
        val cacheId = "edge"
        val cache = Cache(cacheId, cacheId, 0)
        val location = "CDN_in_a_Box_Edge"
        val cacheLocation = CacheLocation(location, Geolocation(38.897663, 38.897663))
        cacheLocation.addCache(cache)
        val locations: MutableSet<CacheLocation> = HashSet()
        locations.add(cacheLocation)
        val register = CacheRegister()
        register.setConfiguredLocations(locations)

        /* Add a capability to the Cache */
        val capability = "a-capability"
        val capabilities: MutableSet<String> = HashSet()
        capabilities.add(capability)
        cache.addCapabilities(capabilities)

        /* Mock a DeliveryService and add it to our DeliveryService Map */
        val dsId = "top-ds"
        val routingName = "cdn"
        val domain = "ds.site.com"
        val topology = "foo"
        val superHackedRegexp = "(.*\\.|^)$dsId\\..*"
        val ds = Mockito.mock(DeliveryService::class.java)
        Mockito.`when`(ds.id).thenReturn(dsId)
        Mockito.`when`(ds.domain).thenReturn(domain)
        Mockito.`when`(ds.getRemap(superHackedRegexp)).thenReturn(domain)
        Mockito.`when`(ds.routingName).thenReturn(routingName)
        Mockito.`when`(ds.topology).thenReturn(topology)
        Mockito.`when`(ds.hasRequiredCapabilities(capabilities)).thenReturn(true)
        Mockito.`when`(ds.isDns).thenReturn(false)
        val dsMap: MutableMap<String, DeliveryService> = HashMap()
        dsMap[dsId] = ds
        val dsMatcher = DeliveryServiceMatcher(ds)
        dsMatcher.addMatch(DeliveryServiceMatcher.Type.HOST, superHackedRegexp, "")
        val dsMatchers = TreeSet<DeliveryServiceMatcher>()
        dsMatchers.add(dsMatcher)
        register.setDeliveryServiceMap(dsMap)
        register.setDeliveryServiceMatchers(dsMatchers)

        /* Parse the Topologies config JSON */
        val mapper = ObjectMapper()
        val allTopologiesJson = mapper.readTree("{\"$topology\":{\"nodes\":[\"$location\"]}}")
        Whitebox.setInternalState(handler, "statTracker", StatTracker())
        Whitebox.invokeMethod<Any>(handler, "parseTopologyConfig", allTopologiesJson, dsMap, register)

        /* Assert that the DeliveryService was assigned to the Cache */
        val dsReferences = cache.deliveryServices
        MatcherAssert.assertThat(dsReferences.size, org.hamcrest.Matchers.equalTo(1))
        MatcherAssert.assertThat(dsReferences.iterator().next().deliveryServiceId, org.hamcrest.Matchers.equalTo(dsId))
    }

    @Test
    @Throws(Exception::class)
    fun testParseLocalizationMethods() {
        val allMethods = arrayOf(
            LocalizationMethod.CZ,
            LocalizationMethod.DEEP_CZ,
            LocalizationMethod.GEO
        )
        val expected: MutableSet<LocalizationMethod> = HashSet()
        expected.addAll(Arrays.asList(*allMethods))
        val mapper = ObjectMapper()
        val allMethodsString = "{\"localizationMethods\": [\"CZ\",\"DEEP_CZ\",\"GEO\"]}"
        val allMethodsJson = mapper.readTree(allMethodsString)
        var actual =
            Whitebox.invokeMethod<Set<LocalizationMethod>>(handler, "parseLocalizationMethods", "foo", allMethodsJson)
        MatcherAssert.assertThat(actual, org.hamcrest.Matchers.equalTo(expected))
        val noMethodsString = "{}"
        val noMethodsJson = mapper.readTree(noMethodsString)
        actual = Whitebox.invokeMethod(handler, "parseLocalizationMethods", "foo", noMethodsJson)
        MatcherAssert.assertThat(actual, org.hamcrest.Matchers.equalTo(expected))
        val nullMethodsString = "{\"localizationMethods\": null}"
        val nullMethodsJson = mapper.readTree(nullMethodsString)
        actual = Whitebox.invokeMethod(handler, "parseLocalizationMethods", "foo", nullMethodsJson)
        MatcherAssert.assertThat(actual, org.hamcrest.Matchers.equalTo(expected))
        val CZMethodsString = "{\"localizationMethods\": [\"CZ\"]}"
        val CZMethodsJson = mapper.readTree(CZMethodsString)
        expected.clear()
        expected.add(LocalizationMethod.CZ)
        actual = Whitebox.invokeMethod(handler, "parseLocalizationMethods", "foo", CZMethodsJson)
        MatcherAssert.assertThat(actual, org.hamcrest.Matchers.equalTo(expected))
    }
}