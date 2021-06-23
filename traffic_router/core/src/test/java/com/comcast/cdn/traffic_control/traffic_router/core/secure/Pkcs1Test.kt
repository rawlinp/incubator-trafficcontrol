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
package com.comcast.cdn.traffic_control.traffic_router.core.secure

import com.comcast.cdn.traffic_control.traffic_router.secure.Pkcs1
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

//import java.security.spec.RSAMultiPrimePrivateCrtKeySpec;
//import java.security.spec.RSAPrivateKeySpec;
class Pkcs1Test {
    var pkcs1RsaPrivateKey = """
         -----BEGIN RSA PRIVATE KEY-----
         MIIEowIBAAKCAQEAqCYjDRdkX1gl0ayYJmMJtrVJnPCEIypy6ObtlIwjgPsevKHd
         MZlE+O1IgR4v3CwR1A/xKSh61Ru+bEggXBbyfSk7eT2v4l6GIN4BaylN4jhZv3IF
         Cjbks5xzM/Fs+PGW2hHNjZ79J6lqI6cl7bCkqcG6lsbfMVK8Y3eccQw+s9V7HMDM
         l83jt5i5t8X1eKFGgkrHwX02XHbY8OEzA75X1VQTvqtV4Azy/SZNjpBcnrYKPptD
         zuvCVLVBl0sm+mu3cqsaGAteP5BSNJhCPUXT+v5FQxLPUVq3AwPF1yIgduD/3UZz
         xl0RUgpWbHx9+Y8tkNweGbNKBdtpkgqm1dI1ZwIDAQABAoIBABsDrYPv6ydKQSEz
         imo4ZRoefAojtgb0TevPFgJUlWumbKS/mIrcZfFcJdbgo63Kwr6AJS2InFtajrhU
         yiYhZanoEu8CkxxaNVBYen/d7e5XQUv5pIeklA+rJfMFaY2BOswkKhMDpQZXOH8r
         3nMWew3u2uxYXQlOkoekctTSs8wuUFC7jPKlRrunDTBCBPZYkTyqHDov4k4NwoTX
         0WMQeFZgXoKJAqcxSDdAGTHImIPK941oKlPHJxEAg6XiAmzJ7ipj8VS2WElu+7Fa
         1SG1U1dD0lMn5oo+B4xw97EW0GzKqcAqOG/pyHy17rjjmEVOkCr/ntJdQVYYS0s9
         +wpRTUkCgYEA2XuBSyfNiU6vslliZBarX6kCLXCfOObzatYR0XpMNSCf+mxfVKzz
         ZWgsY6F6dE/twtJdhpdcnguZXGHXVitPJ5lCTLC14E+POiIItRaypcQZWmfMuWSg
         SbIvWxlokS0liWGa1ENxDze80oSc7KwOdIKEzWh9e/dg4TmYJ45G4csCgYEAxe3j
         b+DP3LvG5WUR9ya+Wtgh5doEwjUzqrqLJqCe0Idp/kM1rhcRTP3VgVS9izmeHEfy
         kTwYGuvHSrWR9RDY8kODHd3MdZpv/HfW2hc4x9bHHmDGfoTrNKD61FvfshD7Um4O
         LTWAXH1MYuRXEOdpyI34J8XA4xqSU4wVRW4AF1UCgYBYgpssKxbLOurmetpAQbmd
         RPtN4vfqAJQwds7pogxB0vVIxbJGk9y6+JqYMa/UhnMNRvApRpC7AZ14q5knyJh+
         VTFWZNSgZcC0uAUzLfmm3Rg0Yuo+yWUymQIM4VpdOzJ7pu2MVaY9u0Ftq+rxp1R6
         tmO19UCcoyEaiIYUEyNl4QKBgQC50xsZ2Y4tpZoZmmdgi95hedNxcdvP3ZURcCve
         ayRPkSLhFYabWIrkptfBoaaGxOR9lsrUsf/LnpsvuAI9e8DCysGZ07f2nbUP6g8s
         GGs1q56sFZ2mAPK2KYD0yQDes/TQsgTbSwSlUPnbSpe3hhwZr7hQ1ue+EB9bEwSR
         d7HcNQKBgA9g/ltyS8gwvP4PfVrwJLrmojJ4u2KW++PDngZTdMUC0GflbIE1qjPa
         RKiKr0t5PB7LJNk9aih2suQhBf+XqBuPWceqzZP7Djxb3980d5JOtgqT1HmyJlqj
         j/mOtWv+25AXx2IzbOo8KT2riNdbJR4lrFFPeGaUuTKcX0cUzsMC
         -----END RSA PRIVATE KEY-----
         """.trimIndent()
    var modulus =
        "21226841249724200470364080122546235817572418012256021428701505610193649007687823990863388086306269875304069894" +
                "35037466585838781300261457808002716628616324521875905526317964734496237147161781756119777212946222036328209383" +
                "84819699187275351664875974826255139859869269335498696511195135561738298955465680468403441729544728368747314571" +
                "19073950298834811621892111493508486073019987048685679846703583256823758223850757202766647631471384317589638563" +
                "05904604406711774794170734625085711041624162182349362888881298038074271747403468747665995211377825593060151428" +
                "1585422378537840908518325974630947888143442629493181547270785611111"
    var primeExponent =
        "34102478221210324969477302838135605371503118734614591699772365620905584692913178631887424380230818547854883793" +
                "52352027966232285321338005899028122035293236017948854736500274759221642876919358547108524081833280714787023605" +
                "37370739085222420568454329454451739869777917334248710738418539501280581915879297137772526550806226447596373541" +
                "14850271100165159619304923378803231802663025836131467802602343604679427390171304863045795266975674828111700694" +
                "15272313188630778957693499734328853197993206205762038507605359981762308537816031605445470812776267900587250491" +
                "631334791818799332176885373984628513100986251103524456834899463497"
    var primeP =
        "15272136415684069158790631123457673629318067540175444562997255698317049262649624390224321247216235244544087089" +
                "62013904155807986715557150582689278103161919123812905971283362519860037397603844960184839875414831064192027964" +
                "30300415302407918795483844776064313830480902099215949465638253735431480569663519283667403"
    var primeQ =
        "13899064722814295473979282784805098517371740260808195745765188168823356822663017143148545214812940925480672971" +
                "20682062911680764022016389159174659010911143729028376737866163291974963587960688594182435348877733530699334627" +
                "41050050362640283000356289151561500233066719984308797295825433506614996971051042906052437"
    var primeExponentP =
        "62153962262405281383533532710509036275870347655760785111351267831226992070645708795956955049170915123133480454" +
                "45295764475595560016073412017866003260377299368347319539507430173750235358482956616575377139183113988148644256" +
                "5100213268013854923373744722327652753201803878576800954384599594601956906084584072111585"
    var primeExponentQ =
        "13049049442758148621671289355116543428526874994388932603585551106085605412845152841918104864766856008116031054" +
                "90615173854354116445834359347770840811791076224169354598229506403892877833950794337724507752584006994680857535" +
                "28450624056073576342049866719661070049595834626463463216424967548537683272530015395372085"
    var crtCoefficient =
        "10799417626813797307460896837148682941454792474724034207255903444299770992480131836697275486741822508924282468" +
                "52458006170555352881146545262410322140189997390028193283710639746267104681600745617023373408764602090442182946" +
                "9476549402421024763445786732141939883673740808445918714027766391963596105763161996444418"

    @Test
    @Throws(Exception::class)
    fun itReadsPkcs1() {
        Security.addProvider(BouncyCastleProvider())
        val pkcs1 = Pkcs1(pkcs1RsaPrivateKey)
        val keySpec = pkcs1.keySpec
        //assertThat(keySpec instanceof RSAMultiPrimePrivateCrtKeySpec, equalTo(true));
        Assert.assertThat(keySpec is PKCS8EncodedKeySpec, Matchers.equalTo(true))
        val rsaKeySpec = keySpec as PKCS8EncodedKeySpec
        //RSAMultiPrimePrivateCrtKeySpec rsaKeySpec = (RSAMultiPrimePrivateCrtKeySpec) keySpec;
        //assertThat(rsaKeySpec.getPrivateExponent(), equalTo(new BigInteger(primeExponent)));
        //assertThat(rsaKeySpec.getPrimeP(), equalTo(new BigInteger(primeP)));
        //assertThat(rsaKeySpec.getPrimeQ(), equalTo(new BigInteger(primeQ)));
        //assertThat(rsaKeySpec.getPrimeExponentP(), equalTo(new BigInteger(primeExponentP)));
        //assertThat(rsaKeySpec.getPrimeExponentQ(), equalTo(new BigInteger(primeExponentQ)));
        //assertThat(rsaKeySpec.getCrtCoefficient(), equalTo(new BigInteger(crtCoefficient)));
        val rsaPrivateKey = pkcs1.privateKey as RSAPrivateKey
        Assert.assertThat(rsaPrivateKey.privateExponent, Matchers.equalTo(BigInteger(primeExponent)))
        Assert.assertThat(rsaPrivateKey.modulus, Matchers.equalTo(BigInteger(modulus)))
        Assert.assertThat(rsaPrivateKey.algorithm, Matchers.equalTo("RSA"))
        // sun.security in rt.jar turns it into pkcs8
        Assert.assertThat(rsaPrivateKey.format, Matchers.equalTo("PKCS#8"))
    }
}