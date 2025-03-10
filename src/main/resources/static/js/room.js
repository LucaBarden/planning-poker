// Global variables
let stompClient = null;
let playerId = null;
let joined = false;
let lastCards = {}
let knownPlayers = {} // Track players we've seen before

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
    let allPlayed = data.players.length > 0 && data.players.every(function(player) {
        return player.card && player.card.trim() !== "";
    });

    if (data.reset) {
        lastCards = {}
    }

    // Get the Reveal Cards button and card area.
    let revealBtn = document.getElementById('revealBtn');
    let cardArea = document.getElementById('cardChoices');
    if (allPlayed && !data.revealed) {
        revealBtn.disabled = false;
        revealBtn.title = "";
    } else {
        revealBtn.disabled = true;
        cardArea.style.display = 'block';
        if (!allPlayed) {
            revealBtn.title = "Warten, dass alle Spieler ihre Karte legen";
        } else if (data.revealed) {
            revealBtn.title = "Die Karten wurden diese Runde bereits aufgedeckt";
        }
    }

    // Process each player's state
    data.players.forEach(function(player) {
        let li = document.createElement('li');
        
        // Check if this is a new player (first time we're seeing this player ID)
        if (!knownPlayers[player.id]) {
            li.className = "player new-player";
            knownPlayers[player.id] = true;
        } else {
            li.className = "player";
        }

        let cardDiv = document.createElement('div');
        cardDiv.className = "card-display";
        let container = document.createElement('div');
        container.className = "card-container";
        cardDiv.appendChild(container);

        // Determine if this player's card is new (i.e. not already animated)
        let isNewCard = false;
        if (player.card && player.card.trim() !== "") {
            if (!lastCards[player.id] || lastCards[player.id] !== player.card) {
                isNewCard = true;
            }
        }

        if (data.revealed) {
            // Hide card selection area when revealed
            cardArea.style.display = 'none';
            // Clear any existing cards in container first
            container.innerHTML = '';
            
            // When cards are revealed, display the card face for everyone with a flip animation if new.
            let cardFace = document.createElement('div');
            if (player.id === playerId) {
                cardFace.className = "card-face"
                cardFace.textContent = player.card;
            } else {
                cardFace.className = "card-back flip-animation";
            }

            cardFace.addEventListener("animationstart", function () {
                setTimeout(() => {
                    cardFace.classList.remove("card-back");
                    cardFace.classList.add("card-face");
                    cardFace.textContent = player.card;
                }, (parseFloat(getComputedStyle(cardFace).animationDuration) * 500))
            });

            cardFace.addEventListener("animationend", function () {
                cardFace.classList.remove("flip-animation");
            }, {once: true});

            container.appendChild(cardFace);
            lastCards[player.id] = player.card;

        } else {
            // Unrevealed state: show placeholder if no card has been played.
            // Clear any existing cards in container first
            container.innerHTML = '';
            
            if (player.card && player.card.trim() !== "") {
                if (player.id === playerId) {
                    // For current user: show the actual card (face) with a throw animation if it's new.
                    let cardFace = document.createElement('div');
                    if (isNewCard) {
                        cardFace.className = "card-face card-throw";
                        cardFace.addEventListener("animationend", function () {
                            cardFace.classList.remove("card-throw");
                        }, {once: true});
                    } else {
                        cardFace.className = "card-face";
                    }
                    cardFace.textContent = player.card;
                    container.appendChild(cardFace);
                    lastCards[player.id] = player.card;
                } else {
                    // For other players: show a generic face-down card (card-back).
                    let cardBack = document.createElement('div');
                    // If it's a new play for them, animate the card-back as if thrown in.
                    if (isNewCard) {
                        cardBack.className = "card-back card-throw";
                        cardBack.addEventListener("animationend", function () {
                            cardBack.classList.remove("card-throw");
                        });
                    } else {
                        cardBack.className = "card-back";
                    }
                    container.appendChild(cardBack);
                    lastCards[player.id] = player.card;
                }
            } else {
                // No card played: show a placeholder.
                let placeholder = document.createElement('div');
                placeholder.className = "card-placeholder";
                container.appendChild(placeholder);
            }
        }

        // Create status container for the player's name and icon.
        let statusContainer = document.createElement('div');
        statusContainer.className = "player-status-container";

        let statusDiv = document.createElement('div');
        statusDiv.className = "player-status";
        statusDiv.innerHTML = (player.card && player.card.trim() !== "") ? "&#9989;" : "&#10060;";

        let nameLabel = document.createElement('span');
        nameLabel.className = "player-name";
        nameLabel.textContent = player.name;

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
    
    // Simulation functions (callable from browser console)
    window.simulatedPlayers = [];
    
    /**
     * Simulates players joining the room with random names
     * Call from browser console with: simulatePlayers(5)
     * @param {number} amount - Number of players to simulate
     */
    window.simulatePlayers = function(amount) {
        if (!joined) {
            console.error("You must join the room first before simulating other players");
            return;
        }
        
        const firstNames = ["Alex", "Jamie", "Taylor", "Jordan", "Casey", "Riley", "Avery", 
                           "Quinn", "Skyler", "Morgan", "Reese", "Blake", "Dakota", "Hayden", 
                           "Parker", "Peyton", "Charlie", "Drew", "Emerson", "Finley"];
        
        const lastNames = ["Smith", "Johnson", "Williams", "Jones", "Brown", "Miller", "Davis", 
                          "Garcia", "Rodriguez", "Wilson", "Martinez", "Anderson", "Taylor", 
                          "Thomas", "Hernandez", "Moore", "Martin", "Lee", "Thompson", "White"];
        
        for (let i = 0; i < amount; i++) {
            const randomFirstName = firstNames[Math.floor(Math.random() * firstNames.length)];
            const randomLastName = lastNames[Math.floor(Math.random() * lastNames.length)];
            const simulatedName = `${randomFirstName} ${randomLastName}`;
            const simulatedId = 'sim-player-' + Math.floor(Math.random() * 1000000);
            
            // Simulate a player joining
            const joinMsg = {
                type: "JOIN",
                roomId: roomId,
                playerId: simulatedId,
                playerName: simulatedName
            };
            
            window.simulatedPlayers.push({
                id: simulatedId,
                name: simulatedName
            });
            
            sendMessage(joinMsg);
            console.log(`Simulated player joined: ${simulatedName} (${simulatedId})`);
        }
        
        console.log(`${amount} simulated players added. Use playSimulation() to have them play cards.`);
    };
    
    /**
     * Makes all simulated players play random cards with a delay between them
     * Call from browser console with: playSimulation()
     */
    window.playSimulation = function() {
        if (window.simulatedPlayers.length === 0) {
            console.error("No simulated players found. Run simulatePlayers(amount) first.");
            return;
        }
        
        const cardValues = ["0", "1/2", "1", "2", "3", "5", "8", "13", "20", "40", "100", "?"];
        
        window.simulatedPlayers.forEach((player, index) => {
            setTimeout(() => {
                const randomCard = cardValues[Math.floor(Math.random() * cardValues.length)];
                const cardMsg = {
                    type: "CARD_PLAYED",
                    roomId: roomId,
                    playerId: player.id,
                    card: randomCard
                };
                
                sendMessage(cardMsg);
                console.log(`Simulated player ${player.name} played card: ${randomCard}`);
            }, 800 * index); // Delay each player's card selection by 800ms
        });
        
        console.log("Simulation started. Players will play cards with a delay between them.");
    };
});