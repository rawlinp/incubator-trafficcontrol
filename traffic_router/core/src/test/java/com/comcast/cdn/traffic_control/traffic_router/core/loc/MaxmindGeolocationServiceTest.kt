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

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.record.Location
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.File
import java.net.InetAddress

@RunWith(PowerMockRunner::class)
class MaxmindGeolocationServiceTest {
    private var service: MaxmindGeolocationService? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        service = MaxmindGeolocationService()
        service!!.init()
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsNullWhenDatabaseNotLoaded() {
        MatcherAssert.assertThat(service!!.isInitialized, CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(service!!.location("192.168.99.100"), IsNull.nullValue())
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsNullWhenDatabaseDoesNotExist() {
        service!!.verifyDatabase(Mockito.mock(File::class.java))
        MatcherAssert.assertThat(service!!.isInitialized, CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(service!!.location("192.168.99.100"), IsNull.nullValue())
        service!!.reloadDatabase()
        MatcherAssert.assertThat(service!!.isInitialized, CoreMatchers.equalTo(false))
        MatcherAssert.assertThat(service!!.location("192.168.99.100"), IsNull.nullValue())
    }

    @PrepareForTest(
        MaxmindGeolocationService::class,
        DatabaseReader.Builder::class,
        Location::class,
        CityResponse::class
    )
    @Test
    @Throws(
        Exception::class
    )
    fun itReturnsALocationWhenTheDatabaseIsLoaded() {
        val databaseFile = Mockito.mock(File::class.java)
        Mockito.`when`(databaseFile.exists()).thenReturn(true)
        val location = PowerMockito.mock(Location::class.java)
        Mockito.`when`(location.latitude).thenReturn(40.0)
        Mockito.`when`(location.longitude).thenReturn(-105.0)
        val cityResponse = PowerMockito.mock(CityResponse::class.java)
        Mockito.`when`(cityResponse.location).thenReturn(location)
        val databaseReader = Mockito.mock(DatabaseReader::class.java)
        Mockito.`when`(databaseReader.city(InetAddress.getByName("192.168.99.100"))).thenReturn(cityResponse)
        val builder = Mockito.mock(DatabaseReader.Builder::class.java)
        Mockito.`when`(builder.build()).thenReturn(databaseReader)
        PowerMockito.whenNew(DatabaseReader.Builder::class.java).withArguments(databaseFile).thenReturn(builder)
        service!!.setDatabaseFile(databaseFile)
        service!!.reloadDatabase()
        MatcherAssert.assertThat(service!!.isInitialized, CoreMatchers.equalTo(true))
        MatcherAssert.assertThat(service!!.location("192.168.99.100"), CoreMatchers.notNullValue())
    }

    @After
    fun after() {
        service!!.destroy()
    }
}