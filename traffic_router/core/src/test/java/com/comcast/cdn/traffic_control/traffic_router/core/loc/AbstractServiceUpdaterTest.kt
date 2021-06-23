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

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

@RunWith(PowerMockRunner::class)
@PrepareForTest(AbstractServiceUpdater::class, HttpURLConnection::class, URL::class, Files::class)
class AbstractServiceUpdaterTest {
    private var connection: HttpURLConnection? = null
    private var databasesDirectory: Path? = null
    private var databasePath: Path? = null
    private var databaseFile: File? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        databaseFile = Mockito.mock(File::class.java)
        Mockito.`when`(databaseFile!!.exists()).thenReturn(true)
        Mockito.`when`(databaseFile!!.lastModified()).thenReturn(1L)
        databasePath = Mockito.mock(Path::class.java)
        Mockito.`when`(databasePath!!.toFile()).thenReturn(databaseFile)
        databasesDirectory = Mockito.mock(Path::class.java)
        Mockito.`when`(databasesDirectory!!.resolve(Matchers.anyString())).thenReturn(databasePath)
        PowerMockito.mockStatic(Files::class.java)
        PowerMockito.`when`(Files.exists(Matchers.any(Path::class.java))).thenReturn(true)
        connection = PowerMockito.mock(HttpURLConnection::class.java)
        Mockito.`when`(connection!!.getHeaderField("ETag")).thenReturn("version-1")
        Mockito.`when`(connection!!.getResponseCode()).thenReturn(304)
        val url = PowerMockito.mock(URL::class.java)
        Mockito.`when`(url.openConnection()).thenReturn(connection)
        PowerMockito.whenNew(URL::class.java).withAnyArguments().thenReturn(url)
    }

    @Test
    @Throws(Exception::class)
    fun itUsesETag() {
        val updater = Updater()
        updater.setDatabasesDirectory(databasesDirectory)
        updater.dataBaseURL = "http://www.example.com"
        updater.updateDatabase()
        Mockito.verify(connection, Mockito.times(0))!!
            .setRequestProperty(Matchers.eq("If-None-Match"), Matchers.anyString())
        Mockito.verify(connection)!!.getHeaderField("ETag")
        updater.updateDatabase()
        Mockito.verify(connection)!!.setRequestProperty(Matchers.eq("If-None-Match"), Matchers.anyString())
        Mockito.verify(connection, Mockito.times(2))!!.getHeaderField("ETag")
    }

    internal inner class Updater : AbstractServiceUpdater() {
        @Throws(IOException::class)
        override fun verifyDatabase(dbFile: File): Boolean {
            return false
        }

        @Throws(IOException::class)
        override fun loadDatabase(): Boolean {
            return false
        }

        override fun isLoaded(): Boolean {
            return true
        }
    }
}