# UDP File Server

## Overview

This is a Java program that allows file transfer via UDP. The program consists of two parts: a server and a client. The server listens for incoming requests from clients and responds accordingly. The client can request a list of files available on the server, download a file from the server, and rate a file on the server.

## Usage

### Server

To start the server, run the following command:

```bash
java FileShareServer --port <port> --directory <directory>
```

Where `<port>` is the port number the server will listen on and `<directory>` is the directory containing the files to be shared.

### Client

To interact with the server, run the following command:

```bash
java FileShareClient --port <port> --address <server> --get <filename>
```

Where `<port>` is the port number the server is listening on, `<server>` is the IP address of the server, and `<filename>` is the name of the file to download.
