package jp.t2v.lab.play2.auth.crypto

import org.scalatest.{FunSpec, MustMatchers}

class SignerSpec extends FunSpec with MustMatchers {
  val signer = new Signer {
    override val secret = "play2.auth.secret"
  }

  describe("Signer"){
    describe("sign") {
      it("should return signature of given authToken using HMAC-SHA1") {
        val signature = signer.sign("authToken")

        signature mustBe "c1e3dd7d8f5ffe920006445fe46c216e12f9b6a7"
      }
    }

    describe("verify") {
      it("should return true when signature of given authToken corresponds given signature") {
        val authToken = "authToken"
        val signature = signer.sign("authToken")

        signer.verify(authToken, signature) mustBe true
      }

      it("should return false when signature of given authToken does not correspond given signature") {
        val authToken = "authToken1"
        val signature = signer.sign("authToken2")

        signer.verify(authToken, signature) mustBe false
      }
    }
  }

}
