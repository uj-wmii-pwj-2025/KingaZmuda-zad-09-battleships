To run code locally:

```bash
cd battleships
```

```bash
mvn clean package
```

```bash
cd src/main/java/kingazm
```

```bash
javac net/Server.java 
javac net/Client.java
java net/Server.java  
```

You should get a similar output:
```
sty 01, 2026 9:19:08 PM kingazm.net.Server start
INFO: Server starting on port: 12345
sty 01, 2026 9:19:08 PM kingazm.net.Server start
INFO: Waiting for clients...
```

In separate terminal windows connect as two clients:
```bash
java net/Client.java
```

You should get a similar output for each window with a client and be able to converse with clients:
```
sty 01, 2026 9:19:59 PM kingazm.net.Client connect
INFO: Starting a client... localhost:12345
sty 01, 2026 9:19:59 PM kingazm.net.Client connect
INFO: Connected to localhost:12345
You:
```

And the server window should also show successfull client connection logs:
```
sty 01, 2026 9:19:59 PM kingazm.net.Server handleClient
INFO: Client connected from: /127.0.0.1:55825
sty 01, 2026 9:20:52 PM kingazm.net.Server handleClient
INFO: Client connected from: /127.0.0.1:55840
```



