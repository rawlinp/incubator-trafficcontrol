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
package com.comcast.cdn.traffic_control.traffic_router.core.hashing

import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService
import com.comcast.cdn.traffic_control.traffic_router.core.ds.Dispersion
import com.comcast.cdn.traffic_control.traffic_router.core.hash.ConsistentHasher
import com.comcast.cdn.traffic_control.traffic_router.core.hash.DefaultHashable
import com.comcast.cdn.traffic_control.traffic_router.core.hash.Hashable
import com.comcast.cdn.traffic_control.traffic_router.core.hash.MD5HashFunction
import com.comcast.cdn.traffic_control.traffic_router.core.hash.NumberSearcher
import com.comcast.cdn.traffic_control.traffic_router.core.request.HTTPRequest
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*

class ConsistentHasherTest {
    @Mock
    var md5HashFunction = MD5HashFunction()

    @Mock
    var numberSearcher = NumberSearcher()

    @InjectMocks
    var hashable1 = DefaultHashable()

    @InjectMocks
    var hashable2 = DefaultHashable()

    @InjectMocks
    var hashable3 = DefaultHashable()
    var hashables: MutableList<DefaultHashable> = ArrayList()

    @InjectMocks
    var consistentHasher: ConsistentHasher? = null
    var trafficRouter: TrafficRouter? = null

    @Before
    fun before() {
        hashable1.generateHashes("hashId1", 100)
        hashable2.generateHashes("hashId2", 100)
        hashable3.generateHashes("hashId3", 100)
        hashables.add(hashable1)
        hashables.add(hashable2)
        hashables.add(hashable3)
        trafficRouter = Mockito.mock(TrafficRouter::class.java)
        Mockito.`when`(trafficRouter!!.buildPatternBasedHashString(Matchers.anyString(), Matchers.anyString()))
            .thenCallRealMethod()
        Mockito.`when`(
            trafficRouter!!.buildPatternBasedHashString(
                Matchers.any(
                    DeliveryService::class.java
                ), Matchers.any(HTTPRequest::class.java)
            )
        ).thenCallRealMethod()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun itHashes() {
        val mapper = ObjectMapper()
        val hashable =
            consistentHasher!!.selectHashable(hashables, Dispersion(mapper.createObjectNode()), "some-string")
        Assert.assertThat(
            hashable,
            org.hamcrest.Matchers.anyOf(
                IsEqual.equalTo(hashable1),
                IsEqual.equalTo(hashable2),
                IsEqual.equalTo(hashable3)
            )
        )
        val nextHashable =
            consistentHasher!!.selectHashable(hashables, Dispersion(mapper.createObjectNode()), "some-string")
        Assert.assertThat(nextHashable, IsEqual.equalTo(hashable))
    }

    @Test
    @Throws(Exception::class)
    fun itHashesMoreThanOne() {
        val jsonStr = """
            {"dispersion": {
            "limit": 2,
            "shuffled": "true"
            }}
            """.trimIndent()
        val mapper = ObjectMapper()
        val jo = mapper.readTree(jsonStr)
        val dispersion = Dispersion(jo)
        val results = consistentHasher!!.selectHashables(hashables, dispersion, "some-string")
        Assert.assertThat(results.size, IsEqual.equalTo(2))
        Assert.assertThat(
            results[0],
            org.hamcrest.Matchers.anyOf(
                IsEqual.equalTo(hashable1),
                IsEqual.equalTo(hashable2),
                IsEqual.equalTo(hashable3)
            )
        )
        Assert.assertThat(
            results[1],
            org.hamcrest.Matchers.anyOf(
                IsEqual.equalTo(hashable1),
                IsEqual.equalTo(hashable2),
                IsEqual.equalTo(hashable3)
            )
        )
        val results2 = consistentHasher!!.selectHashables(hashables, dispersion, "some-string")
        assert(results.containsAll(results2))
        val jsonStr2 = """
            {"dispersion": {
            "limit": 2000000000,
            "shuffled": "true"
            }}
            """.trimIndent()
        val jo2 = mapper.readTree(jsonStr2)
        val disp2 = Dispersion(jo2)
        val res3 = consistentHasher!!.selectHashables(hashables, disp2, "some-string")
        assert(res3.containsAll(hashables))
    }

    @Test
    fun itemsMigrateFromSmallerToLargerBucket() {
        val randomPaths: MutableList<String> = ArrayList()
        for (i in 0..9999) {
            randomPaths.add(generateRandomPath())
        }
        val smallerBucket: Hashable<*> = DefaultHashable().generateHashes("Small One", 10000)
        val largerBucket: Hashable<*> = DefaultHashable().generateHashes("Larger bucket", 90000)
        val buckets: MutableList<Hashable<*>> = ArrayList()
        buckets.add(smallerBucket)
        buckets.add(largerBucket)
        val hashedPaths: MutableMap<Hashable<*>, MutableList<String>> = HashMap()
        hashedPaths[smallerBucket] = ArrayList()
        hashedPaths[largerBucket] = ArrayList()
        val mapper = ObjectMapper()
        for (randomPath in randomPaths) {
            val hashable = consistentHasher!!.selectHashable(buckets, Dispersion(mapper.createObjectNode()), randomPath)
            hashedPaths[hashable]!!.add(randomPath)
        }
        val grownBucket: Hashable<*> = DefaultHashable().generateHashes("Small One", 20000)
        val shrunkBucket: Hashable<*> = DefaultHashable().generateHashes("Larger bucket", 80000)
        val changedBuckets: MutableList<Hashable<*>> = ArrayList()
        changedBuckets.add(grownBucket)
        changedBuckets.add(shrunkBucket)
        val rehashedPaths: MutableMap<Hashable<*>, MutableList<String>> = HashMap()
        rehashedPaths[grownBucket] = ArrayList()
        rehashedPaths[shrunkBucket] = ArrayList()
        for (randomPath in randomPaths) {
            val hashable =
                consistentHasher!!.selectHashable(changedBuckets, Dispersion(mapper.createObjectNode()), randomPath)
            rehashedPaths[hashable]!!.add(randomPath)
        }
        Assert.assertThat(
            rehashedPaths[grownBucket]!!.size, org.hamcrest.Matchers.greaterThan(
                hashedPaths[smallerBucket]!!.size
            )
        )
        Assert.assertThat(
            rehashedPaths[shrunkBucket]!!.size, org.hamcrest.Matchers.lessThan(
                hashedPaths[largerBucket]!!.size
            )
        )
        for (path in hashedPaths[smallerBucket]!!) {
            Assert.assertThat(rehashedPaths[grownBucket]!!.contains(path), IsEqual.equalTo(true))
        }
        for (path in rehashedPaths[shrunkBucket]!!) {
            Assert.assertThat(hashedPaths[largerBucket]!!.contains(path), IsEqual.equalTo(true))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testPatternBasedHashing() {
        // use regex to standardize path
        val regex = "/.*?(/.*?/).*?(.m3u8)"
        val expectedResult = "/some_stream_name1234/.m3u8"
        var requestPath = "/path12341234/some_stream_name1234/some_info4321.m3u8"
        var pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult1 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        requestPath = "/pathasdf1234/some_stream_name1234/some_other_info.m3u8"
        pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult2 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        requestPath = "/path4321fdsa/some_stream_name1234/4321some_info.m3u8"
        pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult3 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        requestPath = "/1234pathfdas/some_stream_name1234/some_info.m3u8"
        pathToHash = trafficRouter!!.buildPatternBasedHashString(regex, requestPath)
        Assert.assertThat(pathToHash, IsEqual.equalTo(expectedResult))
        val hashableResult4 = consistentHasher!!.selectHashable(hashables, null, pathToHash)
        Assert.assertThat(
            hashableResult1,
            org.hamcrest.Matchers.allOf(
                IsEqual.equalTo(hashableResult2),
                IsEqual.equalTo(hashableResult3),
                IsEqual.equalTo(hashableResult4)
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun itHashesQueryParams() {
        val j =
            ObjectMapper().readTree("{\"routingName\":\"edge\",\"coverageZoneOnly\":false,\"consistentHashQueryParams\":[\"test\", \"quest\"]}")
        val d = DeliveryService("test", j)
        val r1 = HTTPRequest()
        r1.path = "/path1234/some_stream_name1234/some_other_info.m3u8"
        r1.queryString = "test=value"
        val r2 = HTTPRequest()
        r2.path = r1.path
        r2.queryString = "quest=other_value"
        val p1 = trafficRouter!!.buildPatternBasedHashString(d, r1)
        val p2 = trafficRouter!!.buildPatternBasedHashString(d, r2)
        assert(p1 != p2)
    }

    var alphanumericCharacters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZYZ"
    var exampleValidPathCharacters = "$alphanumericCharacters/=;()-."
    var random = Random(1462307930227L)
    fun generateRandomPath(): String {
        val pathLength = 60 + random.nextInt(61)
        val stringBuilder = StringBuilder("/")
        for (i in 0..3) {
            val index = random.nextInt(alphanumericCharacters.length)
            stringBuilder.append(alphanumericCharacters[index])
        }
        stringBuilder.append("/")
        for (i in 0 until pathLength) {
            val index = random.nextInt(exampleValidPathCharacters.length)
            stringBuilder.append(exampleValidPathCharacters[index])
        }
        return stringBuilder.toString()
    }
}