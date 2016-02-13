#include <wd/common>

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

	std::unique_ptr<wd::module> sniffer;
	std::unique_ptr<wd::module> pinger;

	wd::receiver recv;
	bool         ready = false;

	while (true) {
		recv.parse();

		// fetch settings
		if (unlikely(!ready)) {
			auto name = recv.next();

			// no more settings
			if (name.is_nil()) {
				ready = true;

				sniffer = wd::make_sniffer(cache);
				pinger  = wd::make_pinger();

				continue;
			}

			if (name == "cache") {
				cache = recv.next().as<std::string>();
			}

			continue;
		}

		int request = recv.next().as<int32_t>();
		int family  = recv.next().as<uint8_t>();
		int command = recv.next().as<uint8_t>();

		switch (family) {
			case wd::command::CONTROL: {
				switch (command) {
					case wd::command::control::CLOSE:
						goto close;
				}

				break;
			}

			case wd::command::SNIFFER:
				sniffer->handle(recv, request, command);
				break;

			case wd::command::PINGER:
				pinger->handle(recv, request, command);
				break;

			default:
				wd::response(wd::command::CONTROL, request, [](auto& packer) {
					packer.pack(wd::command::UNKNOWN);
				});
		}
	}

close:
	return 0;
}
