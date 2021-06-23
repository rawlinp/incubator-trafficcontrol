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
package com.comcast.cdn.traffic_control.traffic_router.core

import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.PatternLayout

object TrafficRouterStart {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        var prefix = System.getProperty("user.dir")
        if (!prefix.endsWith("/core")) {
            prefix += "/core"
        }
        System.setProperty("dns.zones.dir", "$prefix/src/test/var/auto-zones")
        System.setProperty("deploy.dir", "$prefix/src/test")
        System.setProperty("dns.tcp.port", "1053")
        System.setProperty("dns.udp.port", "1053")
        LogManager.getLogger("org.springframework").level = Level.WARN
        val consoleAppender = ConsoleAppender(PatternLayout("%d{ISO8601} [%-5p] %c{4}: %m%n"))
        LogManager.getRootLogger().addAppender(consoleAppender)
        LogManager.getRootLogger().level = Level.INFO
        println("[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Embedded Tomcat loading Traffic Router")
        val catalinaTrafficRouter = CatalinaTrafficRouter("$prefix/src/main/conf/server.xml", "$prefix/src/main/webapp")
        println("[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Starting Traffic Router")
        catalinaTrafficRouter.start()
        println("[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Traffic Router started, press q and <ENTER> to stop")
        while ('q'.toInt() != System.`in`.read()) {
            println("[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>> press q and <ENTER> to stop")
        }
        println("[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Stopping Traffic Router")
        catalinaTrafficRouter.stop()
    }
}