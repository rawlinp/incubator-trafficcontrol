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

import com.comcast.cdn.traffic_control.traffic_router.core.dns.protocol.Protocol
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.ExecutorService

class NameServerMainTest {
    private var executorService: ExecutorService? = null
    private var protocols: MutableList<Protocol?>? = null
    private var p1: Protocol? = null
    private var p2: Protocol? = null
    private var main: NameServerMain? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        p1 = Mockito.mock(Protocol::class.java)
        p2 = Mockito.mock(Protocol::class.java)
        protocols = ArrayList()
        protocols!!.add(p1)
        protocols!!.add(p2)
        executorService = Mockito.mock(ExecutorService::class.java)
        main = NameServerMain()
        main!!.protocols = protocols
        main!!.protocolService = executorService
    }

    @Test
    @Throws(Exception::class)
    fun testDestroy() {
        main!!.destroy()
        Mockito.verify(p1)!!.shutdown()
        Mockito.verify(p2)!!.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun testInit() {
        main!!.init()
        Mockito.verify(executorService)!!.submit(p1)
        Mockito.verify(executorService)!!.submit(p2)
    }
}