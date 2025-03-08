package de.lbarden.planningpoker.websocket;

import de.lbarden.planningpoker.PlanningPokerApplication;
import de.lbarden.planningpoker.model.PokerMessage;
import de.lbarden.planningpoker.model.PokerMessage.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
@Disabled
@SpringBootTest(classes = PlanningPokerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Test basic WebSocket connection")
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
    
    @Test
    @DisplayName("Test WebSocket message subscription")
    public void testWebSocketSubscription() throws Exception {
        // Create a list of transports for SockJS
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        // Use the SockJsClient in the WebSocketStompClient
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Use HTTP URL for SockJS endpoints
        String url = "http://localhost:" + port + "/ws";
        
        // Create a blocking queue to store received messages
        BlockingQueue<PokerMessage> messageQueue = new ArrayBlockingQueue<>(10);
        
        // Connect to the WebSocket endpoint
        StompSession session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
        
        // Subscribe to a topic
        String roomId = "test-room";
        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PokerMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((PokerMessage) payload);
            }
        });
        
        // Send a message to the topic via the application destination
        PokerMessage joinMessage = new PokerMessage();
        joinMessage.setType(MessageType.JOIN);
        joinMessage.setRoomId(roomId);
        joinMessage.setPlayerId("test-player");
        joinMessage.setPlayerName("Test Player");
        
        session.send("/app/room", joinMessage);
        
        // Wait for a response message (timeout after 5 seconds)
        PokerMessage response = messageQueue.poll(5, TimeUnit.SECONDS);
        
        // Verify the response
        assertNotNull(response, "Should receive a response message");
        assertEquals(MessageType.UPDATE, response.getType());
        assertEquals(roomId, response.getRoomId());
        
        // Clean up
        session.disconnect();
    }
    
    @Test
    @DisplayName("Test WebSocket multiple clients")
    public void testMultipleClients() throws Exception {
        // Create a list of transports for SockJS
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        // Use the SockJsClient in the WebSocketStompClient
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Use HTTP URL for SockJS endpoints
        String url = "http://localhost:" + port + "/ws";
        
        // Create a room ID for this test
        String roomId = "multi-client-room";
        
        // Create a shared message queue
        BlockingQueue<PokerMessage> messageQueue = new ArrayBlockingQueue<>(10);
        
        // Connect first client
        StompSession session1 = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
        session1.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PokerMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((PokerMessage) payload);
            }
        });
        
        // Connect second client
        StompSession session2 = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
        session2.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PokerMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((PokerMessage) payload);
            }
        });
        
        // First client joins the room
        PokerMessage joinMessage1 = new PokerMessage();
        joinMessage1.setType(MessageType.JOIN);
        joinMessage1.setRoomId(roomId);
        joinMessage1.setPlayerId("player1");
        joinMessage1.setPlayerName("Player 1");
        
        session1.send("/app/room", joinMessage1);
        
        // Wait for first update
        PokerMessage response1 = messageQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(response1);
        
        // Due to subscription by both clients, we should get a second message
        // (either clear the queue or poll again)
        messageQueue.clear();
        
        // Second client joins the room
        PokerMessage joinMessage2 = new PokerMessage();
        joinMessage2.setType(MessageType.JOIN);
        joinMessage2.setRoomId(roomId);
        joinMessage2.setPlayerId("player2");
        joinMessage2.setPlayerName("Player 2");
        
        session2.send("/app/room", joinMessage2);
        
        // Wait for second update
        PokerMessage response2 = messageQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(response2);
        
        // Verify that the update contains both players
        assertTrue(response2.getPlayers().size() >= 2, "Update should contain at least 2 players");
        
        // Clean up
        session1.disconnect();
        session2.disconnect();
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
        
        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            exception.printStackTrace();
        }
        
        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            exception.printStackTrace();
        }
    }
}
