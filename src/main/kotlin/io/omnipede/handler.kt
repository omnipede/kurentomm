package io.omnipede

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.kurento.client.FaceOverlayFilter
import org.kurento.client.IceCandidate
import org.kurento.client.KurentoClient
import org.kurento.client.MediaPipeline
import org.kurento.client.WebRtcEndpoint
import org.kurento.jsonrpc.JsonUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Component
class MagicMirrorHandler(
    private val kurentoClient: KurentoClient
): TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(MagicMirrorHandler::class.java)
    private val gson = GsonBuilder().create()
    private val users = ConcurrentHashMap<String, UserSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("Connection is established, sessionId: {}", session.id)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info("Connection is closed, closing status: {}, sessionId: {}", status, session.id)
        // Stop session to release session memory
        stop(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            log.info("[Received] {}", message.payload)
            doHandleTextMessage(session, message)
        } catch (t: Throwable) {
            // Error handling
            sendError(session, t.message?: t.localizedMessage)
        }
    }

    private fun doHandleTextMessage(session: WebSocketSession, message: TextMessage) {

        val json = gson.fromJson(message.payload, JsonObject::class.java)
        val messageId = json.get("id").asString

        if (messageId == "start") {
            handleStart(session, json)
            return
        }

        if (messageId == "onIceCandidate") {
            handleOnIceCandidate(session, json)
            return
        }

        // If no handler found
        sendError(session, "Undefined message: ")
    }

    private fun handleStart(session: WebSocketSession, json: JsonObject) {
        val pipeline = kurentoClient.createMediaPipeline()
        val webRtcEndpoint = WebRtcEndpoint.Builder(pipeline).build()

        // Save user session
        val user = UserSession(pipeline, webRtcEndpoint)
        users[session.id] = user

        webRtcEndpoint.addIceCandidateFoundListener { event ->
            val response = JsonObject()
            response.addProperty("id", "iceCandidate")
            response.add("candidate", JsonUtils.toJsonObject(event?.candidate))
            try {
                synchronized(session) {
                    session.sendMessage(TextMessage(response.toString()))
                }
            } catch(e: IOException) {
                log.debug(e.message)
            }
        }

        // Media logic: WebRtcEndpoint -> filter -> WebRtcEndPoint
        val filter = FaceOverlayFilter.Builder(pipeline).build()
        filter.setOverlayedImage(
            "https://raw.githubusercontent.com/Kurento/test-files/main/img/mario-wings.png",
            -0.3f, -1.2f, 1.6f, 1.6f
        )
        webRtcEndpoint.connect(filter)
        filter.connect(webRtcEndpoint)

        // SDP negotiation
        val offer = json.get("sdpOffer").asString
        val answer = webRtcEndpoint.processOffer(offer)
        val response = JsonObject()
        response.addProperty("id", "startResponse")
        response.addProperty("sdpAnswer", answer)

        // Send SDP answer
        synchronized(session) {
            session.sendMessage(TextMessage(response.toString()))
        }
        webRtcEndpoint.gatherCandidates()
    }

    private fun handleOnIceCandidate(session: WebSocketSession, json: JsonObject) {
        val user = users[session.id] ?: return
        val jsonCandidate = json.get("candidate").asJsonObject
        val candidate = IceCandidate(
            jsonCandidate.get("candidate").asString,
            jsonCandidate.get("sdpMid").asString,
            jsonCandidate.get("sdpMLineIndex").asInt
        )
        user.addCandidate(candidate)
    }

    private fun stop(session: WebSocketSession) {
        val user = users.remove(session.id) ?: return
        user.release()
    }

    private fun sendError(session: WebSocketSession, message: String) {
        try {
            val response = JsonObject()
            response.addProperty("id", "error")
            response.addProperty("message", message)
            session.sendMessage(TextMessage(response.toString()))
        } catch (e: IOException) {
            log.error("Exception while sending message", e)
        }
    }
}

class UserSession(
    private val mediaPipeline: MediaPipeline,
    private val webRtcEndpoint: WebRtcEndpoint,
) {
    fun addCandidate(candidate: IceCandidate) {
        webRtcEndpoint.addIceCandidate(candidate)
    }

    fun release() {
        mediaPipeline.release()
    }
}
