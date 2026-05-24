# HoloBot (Java Discord Bot)

HoloBot is a Discord bot written in Java and backed by [JDA (Java Discord API)](https://github.com/discord-jda/JDA). This bot started as a personal project to learn Java and programming in general and is now an active bot that provides its services and features to several Discord servers.

## Features

Coming soon!

## Running locally

### Prerequisites

- Java JDK 25
- Maven
- Discord developer account and a Discord bot token

### Installation

1. Clone this repository:
```bash
git clone https://github.com/adrmrt/holobot.git
```
2. Navigate to the project directory: 
```bash
cd holobot
```
3. Create your local environment file from the example:
```bash
cp .env.example .env 
```
4. Open `.env` and replace the placeholder values with your own, such as the bot token and any other required keys.
5. Build the project:
```bash
mvn clean install
```
6. Run the bot:
- Either run `Bootstrap` from your IDE, or
- Run the built JAR:
```bash
java -jar holobot-VERSION.jar
```
where `VERSION` is the current version defined in `pom.xml`.

## Usage

You can add a Discord bot to your server using the following invitation link where you replace `YOUR_BOT` with the ID of your bot:
```
https://discord.com/oauth2/authorize?client_id=YOUR_BOT&scope=bot
```

The default prefix of this bot is `<`. You can see all the commands with:
```
<help
```

## Contributing

Support and contributions are always welcome. Pull requests are the best way to propose changes to the codebase.

1. Fork the repository and create your branch from `main`.
2. Add code or changes.
3. Ensure the test suite passes.
4. Make sure the code is clean and functional.
5. Open a PR in this repository with your branch.

If you encounter a bug or want to suggest new features, feel free to open a new issue. Please make sure to provide a detailed description, so we don't have to scratch our head.

## License

This project is licensed under the [MIT License](LICENSE).