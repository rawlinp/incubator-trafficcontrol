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

import com.comcast.cdn.traffic_control.traffic_router.core.hash.NumberSearcher
import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Test

class NumberSearcherTest {
    @Test
    fun itFindsClosest() {
        val numbers = arrayOf(1.2, 2.3, 3.4, 4.5, 5.6)
        val numberSearcher = NumberSearcher()
        Assert.assertThat(NumberSearcher.findClosest(numbers, 3.4), IsEqual.equalTo(2))
        Assert.assertThat(NumberSearcher.findClosest(numbers, 1.9), IsEqual.equalTo(1))
        Assert.assertThat(NumberSearcher.findClosest(numbers, 1.3), IsEqual.equalTo(0))
        Assert.assertThat(NumberSearcher.findClosest(numbers, 6.7), IsEqual.equalTo(4))
        Assert.assertThat(NumberSearcher.findClosest(numbers, 0.1), IsEqual.equalTo(0))
    }
}