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
package com.comcast.cdn.traffic_control.traffic_router.core.loc

import com.comcast.cdn.traffic_control.traffic_router.core.loc.NetworkNode.SuperNode
import com.comcast.cdn.traffic_control.traffic_router.core.loc.RegionalGeoRule.PostalsType
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import java.util.regex.Pattern

class RegionalGeoRuleTest {
    @Test
    @Throws(Exception::class)
    fun testIsAllowedCoordinateRanges() {
        val urlRegex = ".*abc.m3u8"
        val ruleType = PostalsType.INCLUDE
        val postals: Set<String> = HashSet()
        val whiteList: NetworkNode = SuperNode()
        val alternateUrl = "/alternate.m3u8"
        val coordinateRanges = ArrayList<RegionalGeoCoordinateRange>()
        val coordinateRange = RegionalGeoCoordinateRange()
        val coordinateRange2 = RegionalGeoCoordinateRange()
        coordinateRange.minLat = 10.0
        coordinateRange.minLon = 165.0
        coordinateRange.maxLat = 22.0
        coordinateRange.maxLon = 179.0
        coordinateRanges.add(coordinateRange)
        coordinateRange2.minLat = 17.0
        coordinateRange2.minLon = -20.0
        coordinateRange2.maxLat = 25.0
        coordinateRange2.maxLon = 19.0
        coordinateRanges.add(coordinateRange2)
        val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlRule = RegionalGeoRule(
            null,
            urlRegex, urlRegexPattern,
            ruleType, postals,
            whiteList, alternateUrl, coordinateRanges
        )
        var allowed: Boolean
        allowed = urlRule.isAllowedCoordinates(11.0, 170.0)
        MatcherAssert.assertThat(allowed, Matchers.equalTo(true))
        allowed = urlRule.isAllowedCoordinates(13.0, 162.0)
        MatcherAssert.assertThat(allowed, Matchers.equalTo(false))
        allowed = urlRule.isAllowedCoordinates(23.0, 22.0)
        MatcherAssert.assertThat(allowed, Matchers.equalTo(false))
        allowed = urlRule.isAllowedCoordinates(23.0, -12.0)
        MatcherAssert.assertThat(allowed, Matchers.equalTo(true))
        allowed = urlRule.isAllowedCoordinates(9.0, 21.0)
        MatcherAssert.assertThat(allowed, Matchers.equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun testMatchesUrl() {
        val urlRegex = ".*abc.m3u8"
        val ruleType = PostalsType.INCLUDE
        val postals: Set<String> = HashSet()
        val whiteList: NetworkNode = SuperNode()
        val alternateUrl = "/alternate.m3u8"
        val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlRule = RegionalGeoRule(
            null,
            urlRegex, urlRegexPattern,
            ruleType, postals,
            whiteList, alternateUrl, null
        )
        var matches: Boolean
        var url = "http://example.com/abc.m3u8"
        matches = urlRule.matchesUrl(url)
        MatcherAssert.assertThat(matches, Matchers.equalTo(true))
        url = "http://example.com/AbC.m3u8"
        matches = urlRule.matchesUrl(url)
        MatcherAssert.assertThat(matches, Matchers.equalTo(true))
        url = "http://example.com/path/ABC.m3u8"
        matches = urlRule.matchesUrl(url)
        MatcherAssert.assertThat(matches, Matchers.equalTo(true))
        url = "http://example.com/cbaabc.m3u8"
        matches = urlRule.matchesUrl(url)
        MatcherAssert.assertThat(matches, Matchers.equalTo(true))
        url = "http://example.com/cba.m3u8"
        matches = urlRule.matchesUrl(url)
        MatcherAssert.assertThat(matches, Matchers.equalTo(false))
        url = "http://example.com/abcabc.m3u8"
        matches = urlRule.matchesUrl(url)
        MatcherAssert.assertThat(matches, Matchers.equalTo(true))
    }

    @Test
    @Throws(Exception::class)
    fun testIsAllowedPostalInclude() {
        val urlRegex = ".*abc.m3u8"
        val ruleType = PostalsType.INCLUDE
        val postals: MutableSet<String> = HashSet()
        postals.add("N6G")
        postals.add("N7G")
        val whiteList: NetworkNode = SuperNode()
        val alternateUrl = "/alternate.m3u8"
        val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlRule = RegionalGeoRule(
            null,
            urlRegex, urlRegexPattern,
            ruleType, postals,
            whiteList, alternateUrl, null
        )
        var allowed: Boolean
        allowed = urlRule.isAllowedPostal("N6G")
        MatcherAssert.assertThat(allowed, Matchers.equalTo(true))
        allowed = urlRule.isAllowedPostal("N7G")
        MatcherAssert.assertThat(allowed, Matchers.equalTo(true))
        allowed = urlRule.isAllowedPostal("N8G")
        MatcherAssert.assertThat(allowed, Matchers.equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun testIsAllowedPostalExclude() {
        val urlRegex = ".*abc.m3u8"
        val ruleType = PostalsType.EXCLUDE
        val postals: MutableSet<String> = HashSet()
        postals.add("N6G")
        postals.add("N7G")
        val whiteList: NetworkNode = SuperNode()
        val alternateUrl = "/alternate.m3u8"
        val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlRule = RegionalGeoRule(
            null,
            urlRegex, urlRegexPattern,
            ruleType, postals,
            whiteList, alternateUrl, null
        )
        var allowed: Boolean
        allowed = urlRule.isAllowedPostal("N6G")
        MatcherAssert.assertThat(allowed, Matchers.equalTo(false))
        allowed = urlRule.isAllowedPostal("N7G")
        MatcherAssert.assertThat(allowed, Matchers.equalTo(false))
        allowed = urlRule.isAllowedPostal("N8G")
        MatcherAssert.assertThat(allowed, Matchers.equalTo(true))
    }

    @Test
    @Throws(Exception::class)
    fun testIsInWhiteList() {
        val urlRegex = ".*abc.m3u8"
        val ruleType = PostalsType.EXCLUDE
        val postals: Set<String> = HashSet()
        val whiteList = SuperNode()
        val location = RegionalGeoRule.WHITE_LIST_NODE_LOCATION
        whiteList.add(NetworkNode("10.74.50.0/24", location))
        whiteList.add(NetworkNode("10.74.0.0/16", location))
        whiteList.add(NetworkNode("192.168.250.1/32", location))
        whiteList.add(NetworkNode("128.128.50.3/32", location))
        whiteList.add(NetworkNode("128.128.50.3/22", location))
        whiteList.add6(NetworkNode("2001:0db8:0:f101::1/64", location))
        whiteList.add6(NetworkNode("2001:0db8:0:f101::1/48", location))
        val alternateUrl = "/alternate.m3u8"
        val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlRule = RegionalGeoRule(
            null,
            urlRegex, urlRegexPattern,
            ruleType, postals,
            whiteList, alternateUrl, null
        )
        var `in`: Boolean
        `in` = urlRule.isIpInWhiteList("10.74.50.12")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("10.75.51.12")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
        `in` = urlRule.isIpInWhiteList("10.74.51.1")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("10.74.50.255")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("192.168.250.1")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("128.128.50.3")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("128.128.50.7")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("128.128.2.1")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
        `in` = urlRule.isIpInWhiteList("2001:0db8:0:f101::2")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("2001:0db8:0:f102::1")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("2001:0db8:1:f101::3")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun testIsInWhiteListInvalidParam() {
        try {
            val urlRegex = ".*abc.m3u8"
            val ruleType = PostalsType.EXCLUDE
            val postals: Set<String> = HashSet()
            val whiteList = SuperNode()
            val location = RegionalGeoRule.WHITE_LIST_NODE_LOCATION
            whiteList.add(NetworkNode("10.256.0.0/10", location))
            //whiteList.add(new NetworkNode("a.b.d.0/10", location));
            val alternateUrl = "/alternate.m3u8"
            val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
            val urlRule = RegionalGeoRule(
                null,
                urlRegex, urlRegexPattern,
                ruleType, postals,
                whiteList, alternateUrl, null
            )
            var `in`: Boolean
            `in` = urlRule.isIpInWhiteList("10.74.50.12")
            MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
            `in` = urlRule.isIpInWhiteList("10.74.51.12")
            MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
            `in` = urlRule.isIpInWhiteList("1.1.50.1")
            MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
            `in` = urlRule.isIpInWhiteList("2001:0db8:1:f101::3")
            MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
        } catch (e: NetworkNodeException) {
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIsInWhiteListGlobalMatch() {
        val urlRegex = ".*abc.m3u8"
        val ruleType = PostalsType.EXCLUDE
        val postals: Set<String> = HashSet()
        val whiteList = SuperNode()
        val location = RegionalGeoRule.WHITE_LIST_NODE_LOCATION
        whiteList.add(NetworkNode("0.0.0.0/0", location))
        val alternateUrl = "/alternate.m3u8"
        val urlRegexPattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlRule = RegionalGeoRule(
            null,
            urlRegex, urlRegexPattern,
            ruleType, postals,
            whiteList, alternateUrl, null
        )
        var `in`: Boolean
        `in` = urlRule.isIpInWhiteList("10.74.50.12")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("10.74.51.12")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("1.1.50.1")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("222.254.254.254")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(true))
        `in` = urlRule.isIpInWhiteList("2001:0db8:1:f101::3")
        MatcherAssert.assertThat(`in`, Matchers.equalTo(false))
    }
}