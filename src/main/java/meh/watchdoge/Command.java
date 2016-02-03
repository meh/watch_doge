package meh.watchdoge;

final public class Command {
	public static final int UNKNOWN = -1;

	public static final int SUCCESS = 0;
	public static final int ERROR   = 1;

	public static final int CONTROL = 0;
	public static final int SNIFFER = 1;

	final static public class Control {
		public static final int PING  = 0;
		public static final int ROOT  = 1;
		public static final int CLOSE = 2;
	}

	final static public class Event {
		public static final int SNIFFER = 0xBADB011;
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
}
