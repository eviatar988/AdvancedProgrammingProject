# Advanced Programming Project

## Authors

- Eviatar Cohen

---

# Overview

This project implements a distributed computational graph using the Publish/Subscribe (Pub/Sub) architecture.

The system allows users to deploy a computational graph from a configuration file and publish values to topics through a web interface.

Whenever a value is published, all dependent agents are automatically activated, compute their output, and publish new values to other topics.

The project also contains a lightweight HTTP server implemented from scratch without external frameworks.

---

# Features

- Publish / Subscribe messaging system
- Dynamic computational graph
- HTTP server implementation
- HTML web interface
- Configuration loader
- Automatic graph deployment
- Parallel execution using ParallelAgent
- Automatic graph visualization

---

# Project Structure

```
src
│
├── configs
│   ├── Config.java
│   ├── GenericConfig.java
│   ├── PlusAgent.java
│   ├── IncAgent.java
│   ├── MinusAgent.java
│   ├── MulAgent.java
│   └── DivAgent.java
│
├── graph
│   ├── Agent.java
│   ├── Graph.java
│   ├── Message.java
│   ├── Node.java
│   ├── Topic.java
│   └── TopicManagerSingleton.java
│
├── server
│   ├── HTTPServer.java
│   ├── MyHTTPServer.java
│   └── RequestParser.java
│
├── servlets
│   ├── Servlet.java
│   ├── HtmlLoader.java
│   ├── ConfLoader.java
│   ├── TopicDisplayer.java
│   └── HttpUtil.java
│
├── views
│   └── HtmlGraphWriter.java
│
└── Main.java
```

---

# Configuration Files

The project loads computational graphs from configuration files located in:

```
files_config/
```

Example:

```
configs.PlusAgent
A,B
C

configs.IncAgent
C
D
```

---

# HTML Files

The web interface is located in

```
files_html/
```

Files:

- index.html
- form.html
- graph.html
- temp.html

---

# How to Run

1. Clone the repository

```
git clone https://github.com/eviatar988/AdvancedProgrammingProject.git
```

2. Open the project in IntelliJ IDEA.

3. Build the project.

4. Run

```
Main.java
```

5. Open your browser:

```
http://localhost:8080/app/index.html
```

6. Upload one of the configuration files.

7. Publish values to topics.

---

# Example

Deploy

```
simple.conf
```

Publish

```
A = 5
B = 8
```

The graph computes

```
C = A + B = 13
D = C + 1 = 14
```

---

# Extra Features

Besides the required implementation, the project also supports additional computational agents:

- PlusAgent
- IncAgent
- MinusAgent
- MulAgent
- DivAgent

This makes the computational graph easily extensible without changing the server implementation.

---

# Technologies

- Java
- HTML
- CSS
- Java Threads
- TCP Sockets
- HTTP
- IntelliJ IDEA
- Git
- GitHub

---

# Authors

Eviatar Cohen
