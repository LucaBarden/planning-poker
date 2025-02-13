// Global variables
let stompClient = null;
let playerId = null;
let joined = false;

function connect(callback) {
    let socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        // Subscribe to updates for this room only
        stompClient.subscribe('/topic/room/' + roomId, function (message) {
            let data = JSON.parse(message.body);
            updateRoom(data);
        });
        if (callback) {
            callback();
        }
    });
}

function updateRoom(data) {
    let playersList = document.getElementById('playersList');
    playersList.innerHTML = "";

    // Check whether every player in the room has played a card.
    // (Assumes that an empty string means "not played.")
    let allPlayed = data.players.length > 0 && data.players.every(function(player) {
        return player.card && player.card.trim() !== "";
    });

    // Get the Reveal Cards button.
    let revealBtn = document.getElementById('revealBtn');
    let cardArea = document.getElementById('cardChoices');
    // Enable the reveal button only if:
    // 1. All players have played a card, AND
    // 2. The cards have not already been revealed.
    if (allPlayed && !data.revealed) {
        revealBtn.disabled = false;
        revealBtn.title = "";
    } else {
        revealBtn.disabled = true;
        cardArea.style.display = 'block';
        // Provide a tooltip message for feedback.
        if (!allPlayed) {
            revealBtn.title = "Warten, dass alle Spieler ihre Karte legen";
        } else if (data.revealed) {
            revealBtn.title = "Die Karten wurden diese Runde bereits aufgedeckt";
        }
    }

    // Update the players list.
    data.players.forEach(function(player) {
        // Create a container for this player.
        let li = document.createElement('li');
        li.className = "player";

        // Create the card display container.
        let cardDiv = document.createElement('div');
        cardDiv.className = "card-display";

        if (data.revealed) {
            // If cards are revealed, show the card face with value.
            let cardFace = document.createElement('div');
            cardArea.style.display = 'none';
            cardFace.className = "card-face flip";
            cardFace.textContent = player.card;
            cardDiv.appendChild(cardFace);
        } else {
            // Cards are not yet revealed.
            if (player.card && player.card.trim() !== "") {
                if (player.id === playerId) {
                    // For the current user, show the actual card they've played.
                    let cardFace = document.createElement('div');
                    cardFace.className = "card-face";
                    cardFace.textContent = player.card;
                    cardDiv.appendChild(cardFace);
                } else {
                    // For other players, show a generic face-down card.
                    let cardBack = document.createElement('div');
                    cardBack.className = "card-back";
                    cardDiv.appendChild(cardBack);
                }
            } else {
                // Player hasn't played: show a placeholder.
                let placeholder = document.createElement('div');
                placeholder.className = "card-placeholder";
                cardDiv.appendChild(placeholder);
            }
        }

        // Create a status container element that always shows the player's name with the status icon.
        let statusContainer = document.createElement('div');
        statusContainer.className = "player-status-container";

        // Create a status icon element.
        let statusDiv = document.createElement('div');
        statusDiv.className = "player-status";
        if (player.card && player.card.trim() !== "") {
            // Check mark for played.
            statusDiv.innerHTML = "&#9989;"; // ✅
        } else {
            // Cross mark for not played.
            statusDiv.innerHTML = "&#10060;"; // ❌
        }

        // Create a label for the player's name.
        let nameLabel = document.createElement('span');
        nameLabel.className = "player-name";
        nameLabel.textContent = player.name;

        // Append the status icon and the player's name.
        statusContainer.appendChild(statusDiv);
        statusContainer.appendChild(nameLabel);

        li.appendChild(cardDiv);
        li.appendChild(statusContainer);
        playersList.appendChild(li);
    });
}

function sendMessage(message) {
    stompClient.send("/app/room", {}, JSON.stringify(message));
}

function leaveRoom() {
    if (!joined) return;

    let leaveMsg = {
        type: "LEAVE",
        roomId: roomId,
        playerId: playerId
    };
    sendMessage(leaveMsg);

    if (stompClient !== null) {
        stompClient.disconnect(() => {
            console.log("Disconnected from room.");
        });
    }
}

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('joinBtn').addEventListener('click', function () {
        let userName = document.getElementById('userName').value.trim();
        if (userName === "") {
            alert("Bitte gib deinen Namen ein");
            return;
        }
        // Generate a random playerId for this session
        playerId = 'player-' + Math.floor(Math.random() * 1000000);
        let joinMsg = {
            type: "JOIN",
            roomId: roomId,
            playerId: playerId,
            playerName: userName
        };
        // Connect and then send the join message once connected.
        connect(function() {
            sendMessage(joinMsg);
        });
        document.getElementById('userForm').style.display = 'none';
        document.getElementById('gameArea').style.display = 'block';
        joined = true;
    });

    // When a card is clicked, send the card selection
    document.querySelectorAll('.card').forEach(function(cardElem) {
        cardElem.addEventListener('click', function () {
            if (!joined) {
                alert("Erst dem Raum beitreten");
                return;
            }
            let cardValue = this.getAttribute('data-value');
            let cardMsg = {
                type: "CARD_PLAYED",
                roomId: roomId,
                playerId: playerId,
                card: cardValue
            };
            sendMessage(cardMsg);
            // Apply a simple flip animation by toggling a CSS class
            this.classList.add('selected');
            setTimeout(() => {
                this.classList.remove('selected');
            }, 500);
        });
    });

    // Reveal all cards in the room
    document.getElementById('revealBtn').addEventListener('click', function () {
        if (!joined) return;
        let revealMsg = {
            type: "REVEAL",
            roomId: roomId
        };
        sendMessage(revealMsg);
    });

    // Reset the room state (clear selections)
    document.getElementById('resetBtn').addEventListener('click', function () {
        if (!joined) return;
        let resetMsg = {
            type: "RESET",
            roomId: roomId
        };
        sendMessage(resetMsg);
    });

    // Copy the room link to clipboard
    document.getElementById('copyRoomLinkBtn').addEventListener('click', function() {
        const roomLink = window.location.href;
        navigator.clipboard.writeText(roomLink).then(() => {
        }).catch(err => {
            alert('Fehler beim kopieren des Links.');
        });
    });

    // Leave the room by clicking on the leave room button
    document.getElementById('leaveRoomBtn').addEventListener('click', function() {
        leaveRoom();
        window.location.href = "/";
    });

    // Leave the room upon closing the browser tab or window
    window.addEventListener("beforeunload", function(event) {
        leaveRoom();
    });
});
