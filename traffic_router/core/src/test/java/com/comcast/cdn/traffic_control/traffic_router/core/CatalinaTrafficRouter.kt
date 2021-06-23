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

import org.apache.catalina.connector.Connector
import org.apache.catalina.core.StandardContext
import org.apache.catalina.core.StandardHost
import org.apache.catalina.core.StandardService
import org.apache.catalina.startup.Catalina
import org.springframework.util.SocketUtils
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

class CatalinaTrafficRouter(serverXmlPath: String, appBase: String?) {
    var catalina: Catalina
    fun start() {
        catalina.isAwait = false
        catalina.start()
    }

    fun stop() {
        catalina.stop()
    }

    init {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1\$tFT%1\$tT.%1\$tL [%4\$s] %5\$s %6\$s%n")
        val logger = Logger.getLogger("")
        val handlers = logger.handlers
        for (handler in handlers) {
            handler.level = Level.FINE
        }
        System.setProperty("dns.tcp.port", "1053")
        System.setProperty("dns.udp.port", "1053")
        System.setProperty("catalina.home", "")
        catalina = Catalina()
        catalina.load(arrayOf("-config", serverXmlPath))

        // Override the port and app base property of server.xml
        val trafficRouterService = catalina.server.findService("traffic_router_core") as StandardService
        val secureConnectorList = Arrays.stream(trafficRouterService.findConnectors())
            .filter { k: Connector -> k.getAttribute("portAttribute") == "SecureApiPort" }
            .collect(Collectors.toList())
        val hasHttpsPort = secureConnectorList.size > 0
        val securePort = if (hasHttpsPort) secureConnectorList[0].port else 0
        val apiPort = Arrays.stream(trafficRouterService.findConnectors())
            .filter { k: Connector -> k.getAttribute("portAttribute") == "ApiPort" }
            .collect(Collectors.toList())[0].port
        val connectors = trafficRouterService.findConnectors()
        for (connector in connectors) {
            if (connector.port == 80) {
                connector.port = System.getProperty("routerHttpPort", "8888").toInt()
            }
            SocketUtils.findAvailableTcpPort()
            if (connector.port == 443) {
                connector.port = System.getProperty("routerSecurePort", "8443").toInt()
            }
            if (connector.port == 3443) {
                connector.port = System.getProperty("secureApiPort", "3443").toInt()
            }
            println("[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Traffic Router listening on port " + connector.port + " " + connector.scheme)
        }
        println(
            "[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Traffic Router listening on DNS port " + System.getProperty(
                "dns.udp.port"
            ) + " udp"
        )
        println(
            "[" + System.currentTimeMillis() + "] >>>>>>>>>>>>>>>> Traffic Router listening on DNS port " + System.getProperty(
                "dns.tcp.port"
            ) + " tcp"
        )
        val standardHost = trafficRouterService.container.findChild("localhost") as StandardHost
        standardHost.appBase = appBase

        // We have to manually set up the default servlet, the Catalina class doesn't do this for us
        val rootContext = standardHost.findChild("") as StandardContext
        val defaultServlet = rootContext.createWrapper()
        defaultServlet.name = "default"
        defaultServlet.servletClass = "org.apache.catalina.servlets.DefaultServlet"
        defaultServlet.addInitParameter("debug", "0")
        defaultServlet.addInitParameter("listings", "false")
        defaultServlet.loadOnStartup = 1
        rootContext.addChild(defaultServlet)
        // set docBase to "" so we can start from the root '/' context
        rootContext.docBase = ""
    }
}