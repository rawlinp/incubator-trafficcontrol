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
package com.comcast.cdn.traffic_control.traffic_router.core.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import java.util.*

class ComparableStringByLengthTest {
    @Test
    fun itDoesNotAllowNullOrEmptyString() {
        try {
            ComparableStringByLength(null)
            Assert.fail("Should have caught IllegalArugmentException")
        } catch (e: IllegalArgumentException) {
            MatcherAssert.assertThat(e.message, Matchers.equalTo("String parameter must be non-null and non-empty"))
        }
        try {
            ComparableStringByLength("")
            Assert.fail("Should have caught IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            MatcherAssert.assertThat(e.message, Matchers.equalTo("String parameter must be non-null and non-empty"))
        }
    }

    @Test
    fun itSortsAscendingToShorterStrings() {
        val strings = arrayOf(
            "a", "ba", "b", "bac", "ab", "abc"
        )
        val set: MutableSet<ComparableStringByLength> = TreeSet<ComparableStringByLength>()
        for (string in strings) {
            set.add(ComparableStringByLength(string))
        }
        val iterator: Iterator<ComparableStringByLength> = set.iterator()
        MatcherAssert.assertThat(iterator.next().toString(), Matchers.equalTo("abc"))
        MatcherAssert.assertThat(iterator.next().toString(), Matchers.equalTo("bac"))
        MatcherAssert.assertThat(iterator.next().toString(), Matchers.equalTo("ab"))
        MatcherAssert.assertThat(iterator.next().toString(), Matchers.equalTo("ba"))
        MatcherAssert.assertThat(iterator.next().toString(), Matchers.equalTo("a"))
        MatcherAssert.assertThat(iterator.next().toString(), Matchers.equalTo("b"))
    }

    @Test
    fun itProperlySupportsEquals() {
        val abc = ComparableStringByLength("abc")
        MatcherAssert.assertThat(abc == abc, Matchers.equalTo(true))
        MatcherAssert.assertThat(abc == ComparableStringByLength("abc"), Matchers.equalTo(true))
        MatcherAssert.assertThat(abc == null, Matchers.equalTo(false))
        MatcherAssert.assertThat(abc.equals(""), Matchers.equalTo(false))
        MatcherAssert.assertThat(abc.equals(1L), Matchers.equalTo(false))
    }

    @Test
    fun itUsesStringFieldForHashcode() {
        MatcherAssert.assertThat(ComparableStringByLength("abc").hashCode(), Matchers.equalTo("abc".hashCode()))
    }
}