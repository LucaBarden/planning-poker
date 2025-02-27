# Planning Poker Project Guidelines

## Build Commands
- Build: `./mvnw clean install`
- Run application: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run single test: `./mvnw test -Dtest=RoomServiceTest#testCreateRoom`
- Run tests in class: `./mvnw test -Dtest=RoomServiceTest`
- Run with specific profile: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

## Code Style Guidelines
- Java version: 17
- Indentation: 4 spaces
- Imports: Package imports first, then third-party imports, static imports last
- Naming: Classes in PascalCase, methods/variables in camelCase
- Use Lombok annotations (@Data, @Getter, @Setter) for model classes
- Spring annotations: @Service, @Controller, @Autowired for DI
- Tests: JUnit 5 with @DisplayName for clarity, setup in @BeforeEach
- Method organization: Public methods first, followed by private helpers
- Error handling: Defensive null checks with conditional returns
- Comments: Use for complex logic only (code should be self-documenting)
- Braces: Opening brace on same line as declaration