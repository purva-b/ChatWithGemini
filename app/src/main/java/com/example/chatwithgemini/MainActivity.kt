package com.example.chatwithgemini

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTextView: TextView

    // OkHttpClient instance to execute network calls.
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure your layout file has inputEditText, sendButton, and chatTextView.
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        chatTextView = findViewById(R.id.chatTextView)

        sendButton.setOnClickListener {
            val query = inputEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                // Display the user's message.
                addMessage("User: $query", isUser = true)
                // Send the query to the Gemini API.
                sendQueryToGemini(query)
            }
        }
    }

    /**
     * Constructs the JSON payload as:
     * {
     *   "contents": [
     *     {
     *       "parts": [
     *         {
     *           "text": "user query"
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    private fun sendQueryToGemini(query: String) {
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", query)
                        })
                    })
                })
            })
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonPayload.toString()
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyCtthTH2-o56Fsqa9ELKaD6dkOqcA4IN6I")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    addMessage("Error: ${e.message}", isUser = false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            addMessage("Error: ${response.message}", isUser = false)
                        }
                    } else {
                        val responseData = response.body?.string() ?: ""
                        val answer = parseAnswerFromResponse(responseData)
                        runOnUiThread {
                            addMessage("Gemini: $answer", isUser = false)
                        }
                    }
                }
            }
        })
    }

    /**
     * Parses the response JSON.
     * Expected structure:
     * {
     *   "candidates": [
     *     {
     *       "content": {
     *         "parts": [
     *           { "text": "response text" }
     *         ]
     *       }
     *     }
     *   ]
     * }
     */
    private fun parseAnswerFromResponse(responseData: String): String {
        return try {
            val jsonObject = JSONObject(responseData)
            val candidates = jsonObject.getJSONArray("candidates")
            candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            "Error parsing response"
        }
    }

    /**
     * Simple function to append a message to the chat view.
     */
    private fun addMessage(message: String, isUser: Boolean) {
        // You can enhance this to use different colors or formatting for user vs. system messages.
        chatTextView.append("$message\n")
    }
}
