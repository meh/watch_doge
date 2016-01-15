#include <android/log.h>

#include <unistd.h>
#include <string>
#include <thread>

#include <msgpack.hpp>
#include <pcap.h>
#include <optional.hpp>

#include <wd/device>
#include <wd/sniffer>

using std::experimental::optional;

#define NEXT() ({          \
	unpacker.next(unpacked); \
	unpacked.get();          \
})

const size_t CHUNK = 256;

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
	msgpack::unpacked unpacked;

	while (true) {
		if (unpacker.nonparsed_size() == 0) {
			size_t consumed = CHUNK;

			while (consumed == CHUNK) {
				unpacker.reserve_buffer(CHUNK);
				consumed = read(0, unpacker.buffer(), CHUNK);
				unpacker.buffer_consumed(consumed);
			}
		}

		int id = NEXT().as<int>();
	}

	return 0;
}
