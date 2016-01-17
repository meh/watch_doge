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

Input Protocol
--------------
+ `command: int`
