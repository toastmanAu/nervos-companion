package com.example.nervoscompanion

import com.example.nervoscompanion.data.CkbAddressParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AddressParserTest {

  @Test
  fun testAddressesFromRfc() {
    val addresses = listOf(
      // 1. New Full format
      "ckb1qzda0cr08m85hc8jlnfp3zer7xulejywt49kt2rr0vthywaa50xwsqdnnw7qkdnnclfkg59uzn8umtfd2kwxceqxwquc4",
      // 2. Legacy short format (SECP256K1 + blake160)
      "ckb1qyqt8xaupvm8837nv3gtc9x0ekkj64vud3jqfwyw5v",
      // 3. Legacy short multisig
      "ckb1qyq5lv479ewscx3ms620sv34pgeuz6zagaaqklhtgg",
      // 4. Legacy full format
      "ckb1qjda0cr08m85hc8jlnfp3zer7xulejywt49kt2rr0vthywaa50xw3vumhs9nvu786dj9p0q5elx66t24n3kxgj53qks"
    )

    for (addr in addresses) {
      try {
        println("Testing address: $addr")
        val script = CkbAddressParser.parseAddress(addr)
        println("SUCCESS: parsed code_hash=${script.codeHash}, hash_type=${script.hashType}, args=${script.args}")
        assertNotNull(script)
      } catch (e: Exception) {
        println("FAILED: $addr -> ${e.message}")
        e.printStackTrace()
        throw e
      }
    }
  }

  @Test
  fun testAddressRoundTrip() {
    val original = "ckb1qzda0cr08m85hc8jlnfp3zer7xulejywt49kt2rr0vthywaa50xwsqdnnw7qkdnnclfkg59uzn8umtfd2kwxceqxwquc4"
    val script = CkbAddressParser.parseAddress(original)
    
    val mainnetAddr = CkbAddressParser.encodeAddress(script.codeHash, script.hashType, script.args, "ckb")
    org.junit.Assert.assertEquals(original, mainnetAddr)
    
    val testnetAddr = CkbAddressParser.encodeAddress(script.codeHash, script.hashType, script.args, "ckt")
    val parsedTestnetScript = CkbAddressParser.parseAddress(testnetAddr)
    org.junit.Assert.assertEquals(script.codeHash, parsedTestnetScript.codeHash)
    org.junit.Assert.assertEquals(script.hashType, parsedTestnetScript.hashType)
    org.junit.Assert.assertEquals(script.args, parsedTestnetScript.args)
  }
}
