# Planning Poker Web Application

This is a **Planning Poker** web application built using **Java Spring Boot 3.4.2**, **HTML**, **JavaScript**, and **CSS**. It enables agile teams to collaboratively estimate the effort required for user stories using a fun and interactive card-based voting system.

## Features

- **Rooms**: Multiple teams can use the app simultaneously in separate rooms.
- **Custom Invite Links**: Each room has a unique invite link for easy access.
- **Live Updates**: Real-time updates ensure all participants see the played cards instantly.
- **Animated Cards**: Cards remain face-down until all players have played and are then revealed with an animation.
- **Player Status Indicators**: A sidebar displays the players in the room with checkmarks (✅) or crosses (❌) to indicate if they have played their card.
- **Reveal Button Control**: The "Reveal Cards" button only becomes active when all players have played and is disabled after use.
- **Copy Room Link**: A button to easily copy the invite link to share with others.

## Technologies Used

- **Backend**: Java, Spring Boot 3.4.2, WebSockets
- **Frontend**: HTML, JavaScript, CSS
- **WebSocket for Real-time Updates**
- **Spring Boot Thymeleaf for Rendering Views**

## Installation & Setup

### Prerequisites
- **Java 17+** (Ensure you have JDK 17 or higher installed)
- **Maven** (Used for building the project)

### Clone the Repository
```sh
 git clone https://github.com/your-username/planning-poker.git
 cd planning-poker
```

### Build and Run the Application
```sh
 mvn clean install
 mvn spring-boot:run
```

### Running with Docker
The application is available on Docker Hub under `lucabarden/planning-poker:latest`.

#### Run with Docker CLI
```sh
docker run -p 8080:8080 lucabarden/planning-poker:latest
```

#### Example Docker Compose File
```yaml
version: '3.8'
services:
  planning-poker:
    image: lucabarden/planning-poker:latest
    ports:
      - "8080:8080"
    restart: unless-stopped
```

### Access the Application
Once the application is running, open your browser and go to:
```
http://localhost:8080
```

## How to Use

1. **Create a Room**: Start a new session by generating a unique room.
2. **Invite Players**: Share the invite link with team members.
3. **Play Cards**: Each player selects a card to estimate a story.
4. **Reveal Cards**: Once all players have chosen, reveal the cards.
5. **Start a New Round**: Reset and play again as needed.

## Contributing
Pull requests are welcome! If you find a bug or have an idea for an improvement, feel free to create an issue.

## License
This project is licensed under the **MIT License**. See the `LICENSE` file for details.

---

**Happy Planning! 🎴**

