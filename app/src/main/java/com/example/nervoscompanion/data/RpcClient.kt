package com.example.nervoscompanion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class RpcClient(private val rpcUrl: String) {

  suspend fun call(method: String, params: List<Any> = emptyList()): String = withContext(Dispatchers.IO) {
    val connection = (URL(rpcUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      connectTimeout = 5000
      readTimeout = 5000
    }

    val requestJson = JSONObject().apply {
      put("id", 1)
      put("jsonrpc", "2.0")
      put("method", method)
      put("params", JSONArray(params))
    }

    connection.outputStream.use { os ->
      OutputStreamWriter(os, "UTF-8").use { writer ->
        writer.write(requestJson.toString())
        writer.flush()
      }
    }

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
      connection.inputStream.bufferedReader().use { it.readText() }
    } else {
      val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
      throw Exception("HTTP Error $responseCode: $errorMsg")
    }
  }
}
