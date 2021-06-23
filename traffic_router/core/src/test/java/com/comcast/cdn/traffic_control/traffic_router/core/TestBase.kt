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

import com.comcast.cdn.traffic_control.traffic_router.shared.DeliveryServiceCertificates
import com.comcast.cdn.traffic_control.traffic_router.shared.DeliveryServiceCertificatesMBean
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext
import org.springframework.util.SocketUtils
import java.lang.management.ManagementFactory
import javax.management.ObjectName

object TestBase {
    private val LOGGER = Logger.getLogger(TestBase::class.java)
    const val monitorPropertiesPath = "src/test/conf/traffic_monitor.properties"
    var context: ApplicationContext? = null
        get() {
            System.setProperty("deploy.dir", "src/test")
            System.setProperty("dns.zones.dir", "src/test/var/auto-zones")
            System.setProperty("dns.tcp.port", SocketUtils.findAvailableTcpPort().toString())
            System.setProperty("dns.udp.port", SocketUtils.findAvailableUdpPort().toString())
            if (field != null) {
                return field
            }
            val platformMBeanServer = ManagementFactory.getPlatformMBeanServer()
            try {
                val objectName = ObjectName(DeliveryServiceCertificatesMBean.OBJECT_NAME)
                platformMBeanServer.registerMBean(DeliveryServiceCertificates(), objectName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val consoleAppender = ConsoleAppender(PatternLayout("%d{ISO8601} [%-5p] %c{4}: %m%n"))
            LogManager.getRootLogger().addAppender(consoleAppender)
            LogManager.getRootLogger().level = Level.WARN
            LOGGER.warn("Initializing context before running integration tests")
            field = FileSystemXmlApplicationContext("src/main/webapp/WEB-INF/applicationContext.xml")
            LOGGER.warn("Context initialized integration tests will now start running")
            return field
        }
        private set
}