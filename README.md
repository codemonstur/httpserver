[![Build Status](https://travis-ci.org/codemonstur/httpserver.svg?branch=master)](https://travis-ci.org/codemonstur/httpserver)
[![GitHub Release](https://img.shields.io/github/release/codemonstur/httpserver.svg)](https://github.com/codemonstur/httpserver/releases)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.codemonstur/httpserver/badge.svg)](http://mvnrepository.com/artifact/com.github.codemonstur/http)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# HTTP server

An HTTP server built on top of common-server.

This code is very simple and should compile with Graal.
I haven't tested this yet.

Features it has:
- Basic support for HTTP/0.9
- Basic support for HTTP/1.0
- Basic support for HTTP/1.1
- Some headers and status codes
- Parsing url encoded forms
- Helpers for request parsing
- Helpers for response sending

Missing features:
- No SSE
- No websockets
- No HTTP/2
- No output compression
- No off-the-shelf handlers for common tasks
- No session store
