package de.lbarden.planningpoker.websocket;

import de.lbarden.planningpoker.PlanningPokerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PlanningPokerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    public void testWebSocketConnection() throws Exception {
        // Create a list of transports for SockJS
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());
        SockJsClient sockJsClient = new SockJsClient(transports);

        // Use the SockJsClient in the WebSocketStompClient
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Use HTTP URL for SockJS endpoints
        String url = "http://localhost:" + port + "/ws";
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1);

        StompSessionHandler sessionHandler = new TestSessionHandler(blockingQueue);
        CompletableFuture<StompSession> futureSession = stompClient.connectAsync(url, sessionHandler);

        // Increase timeout if needed
        StompSession session = futureSession.get(3, TimeUnit.SECONDS);
        assertNotNull(session);
        session.disconnect();
    }

    static class TestSessionHandler extends StompSessionHandlerAdapter {
        private final BlockingQueue<String> blockingQueue;

        public TestSessionHandler(BlockingQueue<String> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("Connected to WebSocket!");
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }
    }
}
