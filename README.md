# Dropbox Business API (Java)

Simple Java program that authenticates with **Dropbox Business API** using OAuth 2.0 and fetches team information.

##  Setup
1. Create a **Dropbox Business App** → [Dropbox App Console](https://www.dropbox.com/developers/apps)  
2. Enable scopes: `team_info.read` and `team_data.member`  
3. Add redirect URI:  http://localhost:8080/callback or http://localhost:45678/callback (these 2 are registered)
4. Note down your **Client ID** and **Client Secret**

##  Run
```bash
mvn clean package or mvn clean install
java -jar target/DropBox-TeamInfo-1.0-SNAPSHOT.jar <CLIENT_ID> <CLIENT_SECRET>

