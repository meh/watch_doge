#include <unistd.h>
#include <string>
#include <thread>
#include <map>

#include <msgpack.hpp>
#include <pcap.h>
#include <optional.hpp>

#include <wd/log>
#include <wd/device>
#include <wd/sniffer>
#include <wd/send>

using std::experimental::optional;

int
main (int argc, char* argv[])
{
	(void) argc;
	(void) argv;

	std::map<int, wd::sniffer> sniffers;
	int                        sniffer_id = 0;

	msgpack::unpacker unpacker;
	msgpack::unpacked unpacked;

	const size_t CHUNK = 256;

	#define NEXT() ({          \
		unpacker.next(unpacked); \
		unpacked.get();          \
	})

	while (true) {
		if (unpacker.nonparsed_size() == 0) {
			size_t consumed = CHUNK;

			while (consumed == CHUNK) {
				unpacker.reserve_buffer(CHUNK);
				consumed = read(0, unpacker.buffer(), CHUNK);
				unpacker.buffer_consumed(consumed);
			}
		}

		int request = NEXT().as<int>();
		int command = NEXT().as<int>();

		switch (command) {
			case wd::command::SNIFFER: {
				int command = NEXT().as<int>();

				switch (command) {
					case wd::command::sniffer::CREATE: {
						auto id     = ++sniffer_id;
						auto ip     = NEXT().as<std::string>();
						auto device = wd::device::find(ip);

						if (!device) {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::sniffer::error::DEVICE_NOT_FOUND);
							});

							continue;
						}

						LOG(LOG_DEBUG, "id=%d device=%s", id, device->c_str());

						sniffers.emplace(std::piecewise_construct,
							std::make_tuple(id),
							std::make_tuple(id, *device));

						wd::response(wd::command::CONTROL, request, [](auto& packer) {
							packer.pack(wd::command::SUCCESS);
						});

						break;
					}

					case wd::command::sniffer::START: {
						auto id = NEXT().as<int>();

						if (sniffers.find(id) == sniffers.end()) {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::sniffer::error::NOT_FOUND);
							});

							continue;
						}

						sniffers.at(id).start(request);

						break;
					}

					case wd::command::sniffer::FILTER: {
						auto id      = NEXT().as<int>();
						auto filter  = NEXT();

						if (sniffers.find(id) == sniffers.end()) {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::sniffer::error::NOT_FOUND);
							});

							continue;
						}

						if (filter.is_nil()) {
							sniffers.at(id).filter(request, optional<std::string>());
						}
						else {
							sniffers.at(id).filter(request, optional<std::string>(filter.as<std::string>()));
						}

						break;
					}
				}
			} break;
		}
	}

	return 0;
}
