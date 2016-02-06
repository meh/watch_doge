package meh.watchdoge.backend.util;

fun address(ip: Int): String? {
	if (ip == 0) {
		return null;
	}

	return "%d.%d.%d.%d".format(
		((ip       ) and 0xff),
		((ip shr  8) and 0xff),
		((ip shr 16) and 0xff),
		((ip shr 24) and 0xff));
}
