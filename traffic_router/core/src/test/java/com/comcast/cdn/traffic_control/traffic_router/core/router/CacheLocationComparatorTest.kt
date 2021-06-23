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

import com.comcast.cdn.traffic_control.traffic_router.core.edge.CacheLocation
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation
import org.junit.Assert
import org.junit.Test

class CacheLocationComparatorTest {
    @Test
    fun testCompareBothLocEqual() {
        val comparator = LocationComparator(Geolocation(1.0, 1.0))
        val loc1 = CacheLocation("loc1", Geolocation(0.0, 0.0))
        val loc2 = CacheLocation("loc2", Geolocation(0.0, 0.0))
        Assert.assertEquals(0, comparator.compare(loc1, loc2).toLong())
        Assert.assertEquals(0, comparator.compare(loc2, loc1).toLong())
    }

    @Test
    fun testCompareBothLocNull() {
        val comparator = LocationComparator(Geolocation(1.0, 1.0))
        val loc1 = CacheLocation("loc1", null)
        val loc2 = CacheLocation("loc2", null)
        Assert.assertEquals(0, comparator.compare(loc1, loc2).toLong())
        Assert.assertEquals(0, comparator.compare(loc2, loc1).toLong())
    }

    @Test
    fun testCompareLocsDifferent() {
        val comparator = LocationComparator(Geolocation(1.0, 1.0))
        val loc1 = CacheLocation("loc1", Geolocation(1.0, 1.0))
        val loc2 = CacheLocation("loc2", Geolocation(0.0, 0.0))
        Assert.assertEquals(-1, comparator.compare(loc1, loc2).toLong())
        Assert.assertEquals(1, comparator.compare(loc2, loc1).toLong())
    }

    @Test
    fun testCompareOneLocNull() {
        val comparator = LocationComparator(Geolocation(1.0, 1.0))
        val loc1 = CacheLocation("loc1", Geolocation(0.0, 0.0))
        val loc2 = CacheLocation("loc2", null)
        Assert.assertEquals(-1, comparator.compare(loc1, loc2).toLong())
        Assert.assertEquals(1, comparator.compare(loc2, loc1).toLong())
    }
}