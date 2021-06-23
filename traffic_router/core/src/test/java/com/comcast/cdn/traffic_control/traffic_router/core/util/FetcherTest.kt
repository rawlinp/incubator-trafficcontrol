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

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@RunWith(PowerMockRunner::class)
@PrepareForTest(Fetcher::class, URL::class, InputStreamReader::class)
@PowerMockIgnore("javax.net.ssl.*")
class FetcherTest {
    @Test
    @Throws(Exception::class)
    fun itChecksIfDataHasChangedSinceLastFetch() {
        val inputStream = PowerMockito.mock(InputStream::class.java)
        val inputStreamReader = PowerMockito.mock(InputStreamReader::class.java)
        PowerMockito.whenNew(InputStreamReader::class.java).withArguments(inputStream).thenReturn(inputStreamReader)
        val bufferedReader = PowerMockito.mock(BufferedReader::class.java)
        Mockito.`when`(bufferedReader.readLine()).thenReturn(null)
        PowerMockito.whenNew(BufferedReader::class.java).withArguments(inputStreamReader).thenReturn(bufferedReader)
        val httpURLConnection = PowerMockito.mock(HttpURLConnection::class.java)
        Mockito.`when`(httpURLConnection.inputStream).thenReturn(inputStream)
        val url = PowerMockito.mock(URL::class.java)
        Mockito.`when`(url.openConnection()).thenReturn(httpURLConnection)
        PowerMockito.whenNew(URL::class.java).withArguments("http://www.example.com").thenReturn(url)
        val fetcher = Fetcher()
        fetcher.fetchIfModifiedSince("http://www.example.com", 123456L)
        Mockito.verify(httpURLConnection).ifModifiedSince = 123456L
    }
}