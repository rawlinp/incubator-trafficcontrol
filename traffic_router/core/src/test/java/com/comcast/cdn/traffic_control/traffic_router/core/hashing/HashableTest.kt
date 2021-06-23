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

import com.comcast.cdn.traffic_control.traffic_router.core.hash.DefaultHashable
import com.comcast.cdn.traffic_control.traffic_router.core.hash.NumberSearcher
import org.hamcrest.Matchers
import org.hamcrest.core.IsNot
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class HashableTest {
    @Mock
    private val numberSearcher = NumberSearcher()

    @InjectMocks
    private val defaultHashable: DefaultHashable? = null

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun itReturnsClosestHash() {
        defaultHashable!!.generateHashes("hash id", 100)
        val hash = defaultHashable.getClosestHash(1.23)
        Assert.assertThat(hash, IsNot.not(Matchers.equalTo(0.0)))
        Assert.assertThat(defaultHashable.getClosestHash(1.23), Matchers.equalTo(hash))
    }
}