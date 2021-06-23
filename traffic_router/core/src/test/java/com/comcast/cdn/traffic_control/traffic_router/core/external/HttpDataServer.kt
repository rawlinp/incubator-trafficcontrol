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
package com.comcast.cdn.traffic_control.traffic_router.core.external

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.io.OutputStream
import java.net.HttpCookie
import java.net.InetAddress
import java.net.InetSocketAddress

//import java.util.logging.Logger;
class HttpDataServer(private val testHttpServerPort: Int) : HttpHandler {
    private var httpServer: HttpServer? = null
    private var receivedSteeringPost = false
    private var receivedCertificatesPost = false
    private var receivedCrConfig2Post = false
    private var receivedCrConfig3Post = false
    private var receivedCrConfig4Post = false

    // Useful for producing an access log
    //	static {
    //		Logger logger = Logger.getLogger("com.sun.net.httpserver");
    //		logger.setLevel(java.util.logging.Level.ALL);
    //
    //		java.util.logging.Handler[] handlers = logger.getHandlers();
    //		for (java.util.logging.Handler handler : handlers) {
    //			handler.setLevel(java.util.logging.Level.ALL);
    //		}
    //	}
    @Throws(IOException::class)
    fun start(port: Int) {
        httpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 10)
        httpServer!!.createContext("/", this)
        httpServer!!.start()
        println(">>>>>>>>>>>>> Started Fake Http Data Server at $port")
    }

    fun stop() {
        println(">>>>>>>>>>>>>> Stopping Fake Http Data Server")
        httpServer!!.stop(10)
        println(">>>>>>>>>>>>>> STOPPED Fake Http Data Server")
    }

    @Throws(IOException::class)
    override fun handle(httpExchange: HttpExchange) {
        Thread(object : Runnable {
            override fun run() {
                if ("POST" == httpExchange.requestMethod) {
                    if (!receivedSteeringPost && "/steering" == httpExchange.requestURI.path) {
                        receivedSteeringPost = true
                    }
                    if (!receivedCertificatesPost && "/certificates" == httpExchange.requestURI.path) {
                        receivedCertificatesPost = true
                    }
                    if (!receivedCrConfig2Post && "/crconfig-2" == httpExchange.requestURI.path) {
                        receivedCrConfig2Post = true
                        receivedCrConfig3Post = false
                        receivedCrConfig4Post = false
                    }
                    if (!receivedCrConfig3Post && "/crconfig-3" == httpExchange.requestURI.path) {
                        receivedCrConfig2Post = false
                        receivedCrConfig3Post = true
                        receivedCrConfig4Post = false
                    }
                    if (!receivedCrConfig4Post && "/crconfig-4" == httpExchange.requestURI.path) {
                        receivedCrConfig2Post = false
                        receivedCrConfig3Post = false
                        receivedCrConfig4Post = true
                    }
                    try {
                        httpExchange.sendResponseHeaders(200, 0)
                    } catch (e: IOException) {
                        println(">>>>> failed acknowledging post")
                    }
                    return
                }
                val uri = httpExchange.requestURI
                var path = uri.path
                if (path.startsWith("/")) {
                    path = path.substring(1)
                }
                val query = uri.query
                if ("json" == query) {
                    path += ".json"
                }
                if ("api/2.0/user/login" == path) {
                    try {
                        val headers = httpExchange.responseHeaders
                        headers["Set-Cookie"] = HttpCookie("mojolicious", "fake-cookie").toString()
                        httpExchange.sendResponseHeaders(200, 0)
                    } catch (e: Exception) {
                        println(">>>> Failed setting cookie")
                    }
                }

                // Pretend that someone externally changed steering.json data
                if (receivedSteeringPost && "api/2.0/steering" == path) {
                    path = "api/2.0/steering2"
                }

                // pretend certificates have not been updated
                if (!receivedCertificatesPost && "api/2.0/cdns/name/thecdn/sslkeys" == path) {
                    path = path.replace("/sslkeys", "/sslkeys-missing-1")
                }
                if (path.contains("CrConfig") && receivedCrConfig2Post) {
                    path = path.replace("CrConfig", "CrConfig2")
                }
                if (path.contains("CrConfig") && receivedCrConfig3Post) {
                    path = path.replace("CrConfig", "CrConfig3")
                }
                if (path.contains("CrConfig") && receivedCrConfig4Post) {
                    path = path.replace("CrConfig", "CrConfig4")
                }
                val inputStream = javaClass.classLoader.getResourceAsStream(path)
                if (inputStream == null) {
                    println(">>> $path not found")
                    val response = "404 (Not Found)\n"
                    var os: OutputStream? = null
                    try {
                        httpExchange.sendResponseHeaders(404, response.length.toLong())
                        os = httpExchange.responseBody
                        os.write(response.toByteArray())
                    } catch (e: Exception) {
                        println("Failed sending 404!: " + e.message)
                    } finally {
                        if (os != null) try {
                            os.close()
                        } catch (e: IOException) {
                            println("Failed closing output stream!: " + e.message)
                        }
                        return
                    }
                }
                if (!path.contains("CrConfig")) {
                    try {
                        httpExchange.responseBody.use { os ->
                            httpExchange.sendResponseHeaders(200, 0)
                            val buffer = ByteArray(0x10000)
                            var count: Int
                            while (inputStream!!.read(buffer).also { count = it } >= 0) {
                                os.write(buffer, 0, count)
                            }
                        }
                    } catch (e: Exception) {
                        println("Failed sending data for " + path + " : " + e.message)
                    }
                } else {
                    try {
                        val buffer = ByteArray(0x10000)
                        val stringBuilder = StringBuilder()
                        while (inputStream!!.read(buffer) >= 0) {
                            stringBuilder.append(String(buffer))
                        }
                        var body = stringBuilder.toString()
                        body = body.replace("localhost:8889".toRegex(), "localhost:$testHttpServerPort")
                        if (path.contains("CrConfig")) {
                            body = body.replace("localhost:8889".toRegex(), "localhost:$testHttpServerPort")
                        }
                        httpExchange.sendResponseHeaders(200, 0)
                        httpExchange.responseBody.write(body.toByteArray())
                        httpExchange.responseBody.close()
                    } catch (e: Exception) {
                        println("Failed sending data for " + path + " : " + e.message)
                    }
                }
                try {
                    inputStream!!.close()
                } catch (e: Exception) {
                    println("Failed closing stream!: " + e.message)
                }
            }
        }).start()
    }
}