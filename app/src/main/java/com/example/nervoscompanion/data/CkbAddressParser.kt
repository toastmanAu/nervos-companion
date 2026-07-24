package com.example.nervoscompanion.data

import java.io.ByteArrayOutputStream

object CkbAddressParser {
  private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
  private val CHARSET_VALUES = IntArray(128) { -1 }.apply {
    for (i in CHARSET.indices) {
      this[CHARSET[i].code] = i
    }
  }

  data class DecodedBech32(
    val hrp: String,
    val payload: ByteArray,
    val isBech32m: Boolean
  )

  data class Script(
    val codeHash: String,
    val hashType: String,
    val args: String
  )

  private fun polymod(values: ByteArray): Int {
    var c = 1
    for (v in values) {
      val c0 = (c ushr 25) and 0xff
      c = ((c and 0x1ffffff) shl 5) xor (v.toInt() and 0xff)
      if ((c0 and 1) != 0) c = c xor 0x3b6a57b2
      if ((c0 and 2) != 0) c = c xor 0x26508e6d
      if ((c0 and 4) != 0) c = c xor 0x1ea119fa
      if ((c0 and 8) != 0) c = c xor 0x3d4233dd
      if ((c0 and 16) != 0) c = c xor 0x2a1462b3
    }
    return c
  }

  private fun hrpExpand(hrp: String): ByteArray {
    val result = ByteArray(hrp.length * 2 + 1)
    for (i in hrp.indices) {
      val c = hrp[i].code
      result[i] = (c ushr 5).toByte()
      result[i + hrp.length + 1] = (c and 31).toByte()
    }
    result[hrp.length] = 0
    return result
  }

  private fun verifyChecksum(hrp: String, data: ByteArray, checksumConst: Int): Boolean {
    val exp = hrpExpand(hrp)
    val values = ByteArray(exp.size + data.size)
    System.arraycopy(exp, 0, values, 0, exp.size)
    System.arraycopy(data, 0, values, exp.size, data.size)
    return polymod(values) == checksumConst
  }

  private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray? {
    var acc = 0
    var bits = 0
    val result = ByteArrayOutputStream()
    val maxv = (1 shl toBits) - 1
    for (value in data) {
      val v = value.toInt() and 0xff
      if (v ushr fromBits != 0) {
        return null
      }
      acc = ((acc shl fromBits) or v)
      bits += fromBits
      while (bits >= toBits) {
        bits -= toBits
        result.write((acc ushr bits) and maxv)
      }
    }
    if (pad) {
      if (bits > 0) {
        result.write((acc shl (toBits - bits)) and maxv)
      }
    } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
      return null
    }
    return result.toByteArray()
  }

  fun decode(address: String): DecodedBech32 {
    if (address.isBlank()) throw IllegalArgumentException("Address cannot be blank")

    // Check mixed case
    var hasLower = false
    var hasUpper = false
    for (c in address) {
      if (c in 'a'..'z') hasLower = true
      if (c in 'A'..'Z') hasUpper = true
    }
    if (hasLower && hasUpper) {
      throw IllegalArgumentException("Mixed-case address is not allowed")
    }

    val lowerAddress = address.lowercase()
    val pos = lowerAddress.lastIndexOf('1')
    if (pos < 1 || pos + 7 > lowerAddress.length) {
      throw IllegalArgumentException("Invalid separator position or address too short")
    }

    val hrp = lowerAddress.substring(0, pos)
    if (hrp != "ckb" && hrp != "ckt") {
      throw IllegalArgumentException("Invalid human-readable part (must be 'ckb' or 'ckt')")
    }

    val dataChars = lowerAddress.substring(pos + 1)
    val data = ByteArray(dataChars.length)
    for (i in dataChars.indices) {
      val c = dataChars[i]
      if (c.code >= 128 || CHARSET_VALUES[c.code] == -1) {
        throw IllegalArgumentException("Invalid character in address: $c")
      }
      data[i] = CHARSET_VALUES[c.code].toByte()
    }

    val isBech32m = verifyChecksum(hrp, data, checksumConst = 0x2bc830a3)
    val isBech32 = if (!isBech32m) verifyChecksum(hrp, data, checksumConst = 1) else false

    if (!isBech32 && !isBech32m) {
      throw IllegalArgumentException("Invalid address checksum")
    }

    val payloadBase32 = data.copyOfRange(0, data.size - 6)
    val payload = convertBits(payloadBase32, 5, 8, false)
      ?: throw IllegalArgumentException("Failed to convert bits of payload")

    return DecodedBech32(hrp, payload, isBech32m)
  }

  private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
  }

  fun parseAddress(address: String): Script {
    val decoded = decode(address)
    val payload = decoded.payload
    if (payload.isEmpty()) {
      throw IllegalArgumentException("Empty payload in address")
    }
    val typeByte = payload[0].toInt() and 0xFF
    return when (typeByte) {
      0x00 -> {
        // New full address format: 0x00 | code_hash (32 bytes) | hash_type (1 byte) | args (variable)
        if (payload.size < 34) throw IllegalArgumentException("Payload too short for full format")
        val codeHashBytes = payload.copyOfRange(1, 33)
        val hashTypeVal = payload[33].toInt() and 0xFF
        val hashType = when (hashTypeVal) {
          0 -> "data"
          1 -> "type"
          2 -> "data1"
          else -> throw IllegalArgumentException("Unknown hash type: $hashTypeVal")
        }
        val argsBytes = payload.copyOfRange(34, payload.size)
        Script(
          codeHash = "0x" + codeHashBytes.toHex(),
          hashType = hashType,
          args = "0x" + argsBytes.toHex()
        )
      }
      0x01 -> {
        // Deprecated short address format: 0x01 | code_hash_index (1 byte) | args (variable)
        if (payload.size < 2) throw IllegalArgumentException("Payload too short for short format")
        val codeHashIndex = payload[1].toInt() and 0xFF
        val argsBytes = payload.copyOfRange(2, payload.size)
        val (codeHash, hashType) = when (codeHashIndex) {
          0x00 -> Pair("0x9bd7e06f3ecf4be0f2fcd2188b23f1b9fcc88e5d4b65a8637b17723bbda3cce8", "type") // SECP256K1 + blake160
          0x01 -> Pair("0x5c5069eb0857efc65e1bca0c07df34c31663b36222383a241fc61d730a08e12e", "type") // SECP256K1 + multisig
          0x02 -> Pair("0xd36e651247d08f7daac4431e5f88bcb155e5d4b65a8637b17723bbda3cce8", "type") // anyone_can_pay
          else -> throw IllegalArgumentException("Unknown code hash index: $codeHashIndex")
        }
        Script(
          codeHash = codeHash,
          hashType = hashType,
          args = "0x" + argsBytes.toHex()
        )
      }
      0x02 -> {
        // Deprecated full format (Data): 0x02 | code_hash (32 bytes) | args (variable)
        if (payload.size < 33) throw IllegalArgumentException("Payload too short for deprecated full format (Data)")
        val codeHashBytes = payload.copyOfRange(1, 33)
        val argsBytes = payload.copyOfRange(33, payload.size)
        Script(
          codeHash = "0x" + codeHashBytes.toHex(),
          hashType = "data",
          args = "0x" + argsBytes.toHex()
        )
      }
      0x04 -> {
        // Deprecated full format (Type): 0x04 | code_hash (32 bytes) | args (variable)
        if (payload.size < 33) throw IllegalArgumentException("Payload too short for deprecated full format (Type)")
        val codeHashBytes = payload.copyOfRange(1, 33)
        val argsBytes = payload.copyOfRange(33, payload.size)
        Script(
          codeHash = "0x" + codeHashBytes.toHex(),
          hashType = "type",
          args = "0x" + argsBytes.toHex()
        )
      }
      else -> throw IllegalArgumentException("Unsupported address format type: $typeByte")
    }
  }

  private fun parseHex(hex: String): ByteArray {
    val clean = if (hex.startsWith("0x")) hex.substring(2) else hex
    val len = clean.length
    val data = ByteArray(len / 2)
    for (i in 0 until len step 2) {
      data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
    }
    return data
  }

  fun encodeAddress(codeHash: String, hashType: String, args: String, hrp: String): String {
    val typeByte = 0x00.toByte()
    val codeHashBytes = parseHex(codeHash)
    val hashTypeByte = when (hashType.lowercase()) {
      "data" -> 0x00.toByte()
      "type" -> 0x01.toByte()
      "data1" -> 0x02.toByte()
      else -> 0x01.toByte()
    }
    val argsBytes = parseHex(args)
    
    val payload = ByteArray(1 + codeHashBytes.size + 1 + argsBytes.size)
    payload[0] = typeByte
    System.arraycopy(codeHashBytes, 0, payload, 1, codeHashBytes.size)
    payload[1 + codeHashBytes.size] = hashTypeByte
    System.arraycopy(argsBytes, 0, payload, 1 + codeHashBytes.size + 1, argsBytes.size)
    
    val payloadBase32 = convertBits(payload, 8, 5, true) 
      ?: throw IllegalArgumentException("Failed to convert bits to 5-bit for address encoding")
      
    val exp = hrpExpand(hrp)
    val values = ByteArray(exp.size + payloadBase32.size + 6)
    System.arraycopy(exp, 0, values, 0, exp.size)
    System.arraycopy(payloadBase32, 0, values, exp.size, payloadBase32.size)
    
    val pm = polymod(values) xor 0x2bc830a3
    val checksum = ByteArray(6)
    for (i in 0 until 6) {
      checksum[i] = ((pm ushr (5 * (5 - i))) and 31).toByte()
    }
    
    val totalData = ByteArray(payloadBase32.size + 6)
    System.arraycopy(payloadBase32, 0, totalData, 0, payloadBase32.size)
    System.arraycopy(checksum, 0, totalData, payloadBase32.size, 6)
    
    val sb = StringBuilder()
    sb.append(hrp)
    sb.append('1')
    for (b in totalData) {
      sb.append(CHARSET[b.toInt() and 31])
    }
    return sb.toString()
  }
}
