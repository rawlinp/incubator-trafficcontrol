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

import com.comcast.cdn.traffic_control.traffic_router.core.edge.Cache
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SteeringGeolocationComparatorTest {
    /*
    This test class assumes some knowledge of United States geography. For reference,
    here are the rough distances looking at a map from left to right:

    Seattle <--- 1300 mi ---> Denver <--- 2000 mi ---> Boston

    */
    private var seattleGeolocation: Geolocation? = null
    private var denverGeolocation: Geolocation? = null
    private var bostonGeolocation: Geolocation? = null
    private var seattleCache: Cache? = null
    private var denverCache: Cache? = null
    private var bostonCache: Cache? = null
    private var seattleTarget: SteeringTarget? = null
    private var seattleTarget2: SteeringTarget? = null
    private var denverTarget: SteeringTarget? = null
    private var bostonTarget: SteeringTarget? = null
    private var seattleResult: SteeringResult? = null
    private var seattleResult2: SteeringResult? = null
    private var denverResult: SteeringResult? = null
    private var bostonResult: SteeringResult? = null
    private var seattleComparator: SteeringGeolocationComparator? = null
    private var denverComparator: SteeringGeolocationComparator? = null
    private var bostonComparator: SteeringGeolocationComparator? = null

    @Before
    fun before() {
        seattleGeolocation = Geolocation(47.0, -122.0)
        denverGeolocation = Geolocation(39.0, -104.0)
        bostonGeolocation = Geolocation(42.0, -71.0)
        seattleCache = Cache("seattle-id", "seattle-hash-id", 1, seattleGeolocation)
        denverCache = Cache("denver-id", "denver-hash-id", 1, denverGeolocation)
        bostonCache = Cache("boston-id", "boston-hash-id", 1, bostonGeolocation)
        seattleTarget = SteeringTarget()
        seattleTarget!!.geolocation = seattleGeolocation
        seattleResult = SteeringResult(seattleTarget, null)
        seattleResult!!.cache = seattleCache
        seattleTarget2 = SteeringTarget()
        seattleTarget2!!.geolocation = seattleGeolocation
        seattleResult2 = SteeringResult(seattleTarget2, null)
        seattleResult2!!.cache = seattleCache
        denverTarget = SteeringTarget()
        denverTarget!!.geolocation = denverGeolocation
        denverResult = SteeringResult(denverTarget, null)
        denverResult!!.cache = denverCache
        bostonTarget = SteeringTarget()
        bostonTarget!!.geolocation = bostonGeolocation
        bostonResult = SteeringResult(bostonTarget, null)
        bostonResult!!.cache = bostonCache
        seattleComparator = SteeringGeolocationComparator(seattleGeolocation)
        denverComparator = SteeringGeolocationComparator(denverGeolocation)
        bostonComparator = SteeringGeolocationComparator(bostonGeolocation)
    }

    @Test
    fun testLeftNullOriginGeo() {
        seattleResult!!.steeringTarget.geolocation = null
        Assert.assertEquals(1, seattleComparator!!.compare(seattleResult, bostonResult).toLong())
    }

    @Test
    fun testRightNullOriginGeo() {
        denverResult!!.steeringTarget.geolocation = null
        Assert.assertEquals(-1, seattleComparator!!.compare(seattleResult, denverResult).toLong())
    }

    @Test
    fun testBothNullOriginGeo() {
        seattleResult!!.steeringTarget.geolocation = null
        denverResult!!.steeringTarget.geolocation = null
        Assert.assertEquals(0, seattleComparator!!.compare(seattleResult, denverResult).toLong())
    }

    @Test
    fun testSameCacheAndOriginGeo() {
        Assert.assertEquals(0, seattleComparator!!.compare(seattleResult, seattleResult).toLong())
    }

    @Test
    fun testSameCacheAndOriginGeoWithGeoOrder() {
        seattleTarget!!.geoOrder = 1
        seattleTarget2!!.geoOrder = 2
        Assert.assertEquals(-1, seattleComparator!!.compare(seattleResult, seattleResult2).toLong())
        Assert.assertEquals(1, seattleComparator!!.compare(seattleResult2, seattleResult).toLong())
        seattleTarget!!.geoOrder = 2
        Assert.assertEquals(0, seattleComparator!!.compare(seattleResult, seattleResult2).toLong())
    }

    @Test
    fun testDifferentCacheAndOriginGeo() {
        Assert.assertEquals(-1, seattleComparator!!.compare(seattleResult, denverResult).toLong())
        Assert.assertEquals(-1, seattleComparator!!.compare(denverResult, bostonResult).toLong())
        Assert.assertEquals(1, seattleComparator!!.compare(bostonResult, seattleResult).toLong())
    }

    @Test
    fun testCacheGeoDifferentFromOriginGeo() {
        seattleResult!!.cache = denverCache
        // seattle -> denver -> seattle || seattle -> denver
        Assert.assertEquals(1, seattleComparator!!.compare(seattleResult, denverResult).toLong())
        // denver -> seattle || denver
        Assert.assertEquals(1, denverComparator!!.compare(seattleResult, denverResult).toLong())
        // boston -> denver -> seattle || boston -> denver
        Assert.assertEquals(1, bostonComparator!!.compare(seattleResult, denverResult).toLong())
        // seattle -> denver -> seattle || seattle -> boston
        Assert.assertEquals(-1, seattleComparator!!.compare(seattleResult, bostonResult).toLong())
        seattleResult!!.cache = bostonCache
        bostonResult!!.cache = denverCache
        // seattle -> boston -> seattle || seattle -> denver -> boston
        Assert.assertEquals(1, seattleComparator!!.compare(seattleResult, bostonResult).toLong())
    }
}