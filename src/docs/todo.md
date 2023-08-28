
- Implement HTTP/2
- Implement SSE
- Implement websockets
- Implement chunked transfer encoding

- Implement 100 continue responses
https://datatracker.ietf.org/doc/html/rfc2616#section-8.2.3
Maybe have the server send it automatically if the app code calls getInputStream() ?

Client must send an Expect header
A body must be present when this happens
