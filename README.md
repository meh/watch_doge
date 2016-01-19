WATCH\_DOGE
===========
;^)

Design
------
```
                            J
                   +----------+
                   | FRONTEND |
                   +----+-----+
                        ^
                      A |
            J           v   J                J
  +----------+  A  +---------+  A  +----------+
  | net.wifi |<--->| SERVICE |<--->| location |
  +----------+     +---------+     +----------+
                        ^
                    msg |
            C           v   C
   +---------+     +---------+
   | libpcap |<--->| BACKEND |
   +---------+     +---------+
```

Protocol
--------
Each request starts:

* `id: int`:   the request id which will be used in the response
* `type: int`: the family type of the command `CONTROL` or `SNIFFER`

Each response starts with:

* `type: int`: the family type of the command `CONTROL` or `SNIFFER`
* `id: int`:   the request id in case of `CONTROL` and the sniffer id in case of `SNIFFER`
