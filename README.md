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

### Request

* `id:      u32`: the request id which will be used in the response
* `family:   u8`:  the family of the command `CONTROL` or `SNIFFER`
* `command:  u8`:  the command id

Additional parameters are command dependant.

### Response

* `family:  u8`: it's always `CONTROL`
* `id:     u32`: the request id
* `status:  u8`: the response status

Additional response values are command dependant.

### Sniffer packets

* `family:  u8`: it's always `SNIFFER`
* `id:      u8`: the sniffer id
* `size:   u32`: the packet size
* `secs:   i64`: the packet timestamp seconds
* `msecs:  i64`: the packet timestamp microseconds
