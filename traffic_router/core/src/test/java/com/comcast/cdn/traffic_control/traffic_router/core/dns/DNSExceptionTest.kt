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

import org.junit.Assert
import org.junit.Test
import org.xbill.DNS.Rcode

class DNSExceptionTest {
    @Test
    fun testDNSExceptionInt() {
        val rcode = Rcode.NXDOMAIN
        val ex = DNSException(rcode)
        Assert.assertEquals(rcode.toLong(), ex.rcode.toLong())
    }

    @Test
    fun testDNSExceptionIntString() {
        val rcode = Rcode.NXDOMAIN
        val msg = "message"
        val ex = DNSException(rcode, msg)
        Assert.assertEquals(rcode.toLong(), ex.rcode.toLong())
        Assert.assertEquals(msg, ex.message)
    }

    @Test
    fun testDNSExceptionIntStringThrowable() {
        val rcode = Rcode.NXDOMAIN
        val msg = "message"
        val cause = Exception()
        val ex = DNSException(rcode, msg, cause)
        Assert.assertEquals(rcode.toLong(), ex.rcode.toLong())
        Assert.assertEquals(msg, ex.message)
        Assert.assertEquals(cause, ex.cause)
    }

    @Test
    fun testDNSExceptionIntThrowable() {
        val rcode = Rcode.NXDOMAIN
        val cause = Exception()
        val ex = DNSException(rcode, cause)
        Assert.assertEquals(rcode.toLong(), ex.rcode.toLong())
        Assert.assertEquals(cause, ex.cause)
    }
}