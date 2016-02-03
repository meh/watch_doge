#include <wd/device>
#include <wd/sniffer>

int
main (int argc, char* argv[])
{
	(void) argc;
	(void) argv;

	if (geteuid() == 0) {
		wd::send([](auto& packer) {
			packer.pack_true();
		});
	}
	else {
		wd::send([](auto& packer) {
				packer.pack_false();
		});

		return 1;
	}

	std::string cache;

	std::map<int, wd::sniffer> sniffers;
	int                        sniffer_id = 0;

	msgpack::unpacker unpacker;
	msgpack::unpacked unpacked;
	bool              ready = false;

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

		if (!ready) {
			auto name = NEXT();

			if (name.is_nil()) {
				ready = true;
				continue;
			}

			if (name == "cache") {
				cache = NEXT().as<std::string>();
			}

			continue;
		}

		int request = NEXT().as<int32_t>();
		int family  = NEXT().as<uint8_t>();

		switch (family) {
			case wd::command::CONTROL: {
				int command = NEXT().as<uint8_t>();

				switch (command) {
					case wd::command::control::CLOSE:
						goto close;
				}

				break;
			}

			case wd::command::SNIFFER: {
				int command = NEXT().as<uint8_t>();

				switch (command) {
					case wd::command::sniffer::CREATE: {
						auto id       = ++sniffer_id;
						auto truncate = NEXT().as<uint64_t>();
						auto ip       = NEXT();
						auto device   = std::string("any");

						if (!ip.is_nil()) {
							if (auto dev = wd::device::find(ip.as<std::string>())) {
								device = *dev;
							}
							else {
								wd::response(wd::command::CONTROL, request, [](auto& packer) {
									packer.pack(wd::command::sniffer::error::DEVICE_NOT_FOUND);
								});

								continue;
							}
						}

						sniffers.emplace(std::piecewise_construct,
							std::make_tuple(id),
							std::make_tuple(id, device, cache, truncate));

						wd::response(wd::command::CONTROL, request, [&](auto& packer) {
							packer.pack(wd::command::SUCCESS);
							packer.pack(id);
						});

						break;
					}

					case wd::command::sniffer::START: {
						auto id = NEXT().as<int32_t>();

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
						auto id      = NEXT().as<int32_t>();
						auto filter  = NEXT();

						if (sniffers.find(id) == sniffers.end()) {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::sniffer::error::NOT_FOUND);
							});

							continue;
						}

						if (filter.is_nil()) {
							sniffers.at(id).filter(request, std::nullopt);
						}
						else {
							sniffers.at(id).filter(request, filter.as<std::string>());
						}

						break;
					}

					case wd::command::sniffer::GET: {
						auto id  = NEXT().as<int32_t>();
						auto pid = NEXT().as<int32_t>();

						if (sniffers.find(id) == sniffers.end()) {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::sniffer::error::NOT_FOUND);
							});

							continue;
						}

						if (auto packet = sniffers.at(id).get(pid)) {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::SUCCESS);
							});

							wd::response(wd::command::SNIFFER, id, [&](auto& packer) {
								wd::packet::pack(packer, pid, std::get<0>(*packet), std::get<1>(*packet));
							});
						}
						else {
							wd::response(wd::command::CONTROL, request, [](auto& packer) {
								packer.pack(wd::command::sniffer::error::NOT_FOUND);
							});
						}

						break;
					}
				}

				break;
			}
		}
	}

close:
	return 0;
}
