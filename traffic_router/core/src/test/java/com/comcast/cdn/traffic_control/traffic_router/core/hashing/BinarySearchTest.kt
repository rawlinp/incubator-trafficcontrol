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

import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Test
import java.util.*

class BinarySearchTest {
    @Test
    fun itReturnsMatchingIndex() {
        val hashes = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        Assert.assertThat(Arrays.binarySearch(hashes, 3.0), IsEqual.equalTo(2))
    }

    @Test
    fun itReturnsInsertionPoint() {
        val hashes = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        Assert.assertThat(Arrays.binarySearch(hashes, 3.5), IsEqual.equalTo(-4))
        Assert.assertThat(Arrays.binarySearch(hashes, 4.01), IsEqual.equalTo(-5))
    }
}