#include <unistd.h>

#include <string>
#include <iostream>
#include <sstream>
#include <thread>

#include <msgpack.hpp>
#include <pcap.h>
#include <optional.hpp>

#include <wd/device>
#include <wd/sniffer>

using std::experimental::optional;

int
main (int argc, char* argv[])
{
	(void) argc;
	(void) argv;

	std::map<int, wd::sniffer> sniffers;

#if 0
	wd::sniffer test(1, "wlan0");
	test.start();
	test.filter(optional<std::string>("icmp"));
#endif

	msgpack::unpacker unpacker;

	while (true) {
		unpacker.reserve_buffer(256);
		unpacker.buffer_consumed(std::cin.readsome(unpacker.buffer(), 256));

		msgpack::unpacked result;
		while (unpacker.next(result)) {
			msgpack::object message(result.get());
		}
	}

	return 0;
}
