package meh.watchdoge.backend;

final public class Command {
	public static final int UNKNOWN = -1;

	public static final int SUCCESS = 0;
	public static final int ERROR   = 1;

	public static final int CONTROL  = 0;
	public static final int SNIFFER  = 1;
	public static final int WIRELESS = 2;
	public static final int PINGER   = 3;
	public static final int TRACER   = 4;

	final static public class Control {
		public static final int PING  = 0;
		public static final int ROOT  = 1;
		public static final int CLOSE = 2;
	}

	final static public class Event {
		public static final int SNIFFER  = 0xBADB011;
		public static final int WIRELESS = 0xBADB012;
		public static final int PINGER   = 0xBADB013;
		public static final int TRACER   = 0xBADB014;

		final static public class Sniffer {
			public static final int PACKET = 1;
		}

		final static public class Wireless {
			public static final int STATUS = 1;
		}

		final static public class Pinger {
			public static final int STATS  = 1;
			public static final int PACKET = 2;
			public static final int ERROR  = 3;
		}

		final static public class Tracer {
			public static final int DONE = 1;
			public static final int HOP  = 2;
		}
	}

	final static public class Sniffer {
		final static public class Error {
			public static final int ALREADY_STARTED = 2;
			public static final int NOT_STARTED     = 3;

			public static final int ALREADY_EXISTS   = 4;
			public static final int NOT_FOUND        = 5;
			public static final int DEVICE_NOT_FOUND = 6;
			public static final int INVALID_FILTER   = 7;
		}

		public static final int SUBSCRIBE   = 1;
		public static final int UNSUBSCRIBE = 2;
		public static final int LIST        = 3;

		public static final int CREATE  = 100;
		public static final int DESTROY = 101;
		public static final int START   = 102;
		public static final int STOP    = 103;
		public static final int FILTER  = 104;
		public static final int GET     = 105;
	}

	final static public class Wireless {
		public static final int STATUS      = 1;
		public static final int SUBSCRIBE   = 2;
		public static final int UNSUBSCRIBE = 3;
	}

	final static public class Pinger {
		final static public class Error {
			public static final int ALREADY_STARTED = 2;
			public static final int NOT_STARTED     = 3;

			public static final int ALREADY_EXISTS   = 4;
			public static final int NOT_FOUND        = 5;
			public static final int UNKNOWN_HOST     = 6;
			public static final int SOCKET           = 7;
			public static final int INVALID_INTERVAL = 8;
		}

		public static final int SUBSCRIBE   = 1;
		public static final int UNSUBSCRIBE = 2;

		public static final int CREATE  = 100;
		public static final int START   = 101;
		public static final int STOP    = 102;
		public static final int DESTROY = 103;
	}

	final static public class Tracer {
		final static public class Error {
			public static final int ALREADY_STARTED = 2;
			public static final int NOT_STARTED     = 3;

			public static final int ALREADY_EXISTS = 4;
			public static final int NOT_FOUND      = 5;
			public static final int UNKNOWN_HOST   = 6;
			public static final int SOCKET         = 7;
		}

		public static final int SUBSCRIBE   = 1;
		public static final int UNSUBSCRIBE = 2;

		public static final int CREATE  = 100;
		public static final int START   = 101;
		public static final int STOP    = 102;
		public static final int DESTROY = 103;
	}
}
