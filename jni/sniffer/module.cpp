#include <wd/common>
#include <wd/device>

#include "sniffer"

class sniffer: public wd::module {
	public:
		sniffer(std::string cache) : _cache(cache) { }

		void handle(wd::receiver& recv, int request, int command);

		void create(wd::receiver& recv, int request);
		void start(wd::receiver& recv, int request, int id);
		void filter(wd::receiver& recv, int request, int id);
		void get(wd::receiver& recv, int request, int id);

	private:
		std::string _cache;

		int                        _id;
		std::map<int, wd::sniffer> _map;
};

void
sniffer::handle(wd::receiver& recv, int request, int command)
{
	int id = 0;

	switch (command) {
		case wd::command::sniffer::START:
		case wd::command::sniffer::FILTER:
		case wd::command::sniffer::GET: {
			id = recv.next().as<int32_t>();

			if (_map.find(id) == _map.end()) {
				wd::response(wd::command::CONTROL, request, [](auto& packer) {
					packer.pack(wd::command::sniffer::error::NOT_FOUND);
				});

				return;
			}
		}
	}

	switch (command) {
		case wd::command::sniffer::CREATE:
			create(recv, request);
			break;

		case wd::command::sniffer::START:
			start(recv, request, id);
			break;

		case wd::command::sniffer::FILTER:
			filter(recv, request, id);
			break;

		case wd::command::sniffer::GET:
			get(recv, request, id);
			break;

		default:
			wd::response(wd::command::CONTROL, request, [](auto& packer) {
				packer.pack(wd::command::UNKNOWN);
			});
	}
}

void
sniffer::create(wd::receiver& recv, int request)
{
	auto id       = ++_id;
	auto truncate = recv.next().as<uint64_t>();
	auto ip       = recv.next();
	auto device   = std::string("any");

	if (!ip.is_nil()) {
		if (auto dev = wd::device::find(ip.as<std::string>())) {
			device = *dev;
		}
		else {
			wd::response(wd::command::CONTROL, request, [](auto& packer) {
				packer.pack(wd::command::sniffer::error::DEVICE_NOT_FOUND);
			});

			return;
		}
	}

	_map.emplace(std::piecewise_construct,
		std::make_tuple(id),
		std::make_tuple(id, device, _cache, truncate));

	wd::response(wd::command::CONTROL, request, [&](auto& packer) {
		packer.pack(wd::command::SUCCESS);
		packer.pack(id);
	});
}

void
sniffer::start(wd::receiver& recv, int request, int id)
{
	_map.at(id).start(request);
}

void
sniffer::filter(wd::receiver& recv, int request, int id)
{
	auto filter = recv.next();

	if (filter.is_nil()) {
		_map.at(id).filter(request, std::nullopt);
	}
	else {
		_map.at(id).filter(request, filter.as<std::string>());
	}
}

void
sniffer::get(wd::receiver& recv, int request, int id)
{
	auto pid = recv.next().as<int32_t>();

	if (auto packet = _map.at(id).get(pid)) {
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
}

namespace wd {
	std::unique_ptr<module>
	make_sniffer(std::string cache)
	{
		return std::unique_ptr<module>(new ::sniffer(cache));
	}
}
