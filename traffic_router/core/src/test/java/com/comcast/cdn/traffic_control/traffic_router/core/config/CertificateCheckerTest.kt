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
package com.comcast.cdn.traffic_control.traffic_router.core.config

import com.comcast.cdn.traffic_control.traffic_router.shared.Certificate
import com.comcast.cdn.traffic_control.traffic_router.shared.CertificateData
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.*

class CertificateCheckerTest {
    private var deliveryServicesJson: JsonNode? = null
    private var certificateDataList: List<CertificateData>? = null
    private var certificateData: CertificateData? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        val certificate = Certificate()
        certificate.crt = "the-crt"
        certificate.key = "the-key"
        certificateData = CertificateData()
        certificateData!!.hostname = "https-delivery-service.thecdn.example.com"
        certificateData!!.deliveryservice = "https-delivery-service"
        certificateData!!.certificate = certificate
        certificateDataList = Arrays.asList<CertificateData>(
            certificateData
        )
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsFalseWhenDeliveryServiceNameIsNull() {
        val file = File("src/test/resources/deliveryServices_missingDSName.json")
        val mapper = ObjectMapper()
        deliveryServicesJson = mapper.readTree(file)
        val certificateChecker = CertificateChecker()
        certificateData!!.deliveryservice = null
        MatcherAssert.assertThat(
            certificateChecker.certificatesAreValid(certificateDataList, deliveryServicesJson),
            Matchers.equalTo(false)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsFalseWhenDeliveryServiceNameIsBlank() {
        val file = File("src/test/resources/deliveryServices_missingDSName.json")
        val mapper = ObjectMapper()
        deliveryServicesJson = mapper.readTree(file)
        val certificateChecker = CertificateChecker()
        certificateData!!.deliveryservice = ""
        MatcherAssert.assertThat(
            certificateChecker.certificatesAreValid(certificateDataList, deliveryServicesJson),
            Matchers.equalTo(false)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsTrueWhenAllHttpsDeliveryServicesHaveCertificates() {
        val file = File("src/test/resources/deliveryServices.json")
        val mapper = ObjectMapper()
        deliveryServicesJson = mapper.readTree(file)
        val certificateChecker = CertificateChecker()
        MatcherAssert.assertThat(
            certificateChecker.certificatesAreValid(certificateDataList, deliveryServicesJson),
            Matchers.equalTo(true)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itReturnsFalseWhenAnyHttpsDeliveryServiceMissingCertificates() {
        val file = File("src/test/resources/deliveryServices_missingCert.json")
        val mapper = ObjectMapper()
        deliveryServicesJson = mapper.readTree(file)
        MatcherAssert.assertThat(
            CertificateChecker().certificatesAreValid(certificateDataList, deliveryServicesJson),
            Matchers.equalTo(false)
        )
    }
}