package meh.watchdoge.backend;

final public class Command {
	public static final int SUCCESS = 0;
	public static final int ERROR   = 1;

	public static final int CONTROL = 0;
	public static final int SNIFFER = 1;

	final static public class Sniffer {
		final static public class Error {
			public static final int ALREADY_STARTED = 2;
			public static final int NOT_STARTED     = 3;

			public static final int ALREADY_EXISTS   = 4;
			public static final int NOT_FOUND        = 5;
			public static final int DEVICE_NOT_FOUND = 6;
			public static final int INVALID_FILTER   = 7;
		}

		public static final int CREATE  = 0;
		public static final int DESTROY = 1;
		public static final int LIST    = 2;

		public static final int START   = 3;
		public static final int STOP    = 4;
		public static final int FILTER  = 5;
	}
}
