#include <wd/common>
#include "pinger"

class pinger: public wd::module, public wd::creator<> {
	public:
		void handle(wd::receiver& recv, int request, int command);

		void ok(int id, int request);
		void err(int id, int request, int status);

		void create(wd::receiver& recv, int request);
		void start(wd::receiver& recv, int request, int id);
		void stop(wd::receiver& recv, int request, int id);
		void destroy(wd::receiver& recv, int request, int id);

	private:
		int                       _id;
		std::map<int, wd::pinger> _map;
		std::shared_timed_mutex   _mutex;
};

void
pinger::handle(wd::receiver& recv, int request, int command)
{
	int id = 0;

	switch (command) {
		case wd::command::pinger::START:
		case wd::command::pinger::STOP:
		case wd::command::pinger::DESTROY: {
			id = recv.next().as<int32_t>();

			std::shared_lock<std::shared_timed_mutex> lock(_mutex);
			if (_map.find(id) == _map.end()) {
				wd::response(wd::command::CONTROL, request, [](auto& packer) {
					packer.pack(wd::command::pinger::error::NOT_FOUND);
				});

				return;
			}
		}
	}

	switch (command) {
		case wd::command::pinger::CREATE:
			create(recv, request);
			break;

		case wd::command::pinger::START:
			start(recv, request, id);
			break;

		case wd::command::pinger::STOP:
			stop(recv, request, id);
			break;

		case wd::command::pinger::DESTROY:
			destroy(recv, request, id);
			break;

		default:
			wd::response(wd::command::CONTROL, request, [](auto& packer) {
				packer.pack(wd::command::UNKNOWN);
			});
	}
}

void
pinger::ok(int id, int request)
{
	wd::response(wd::command::CONTROL, request, [&](auto& packer) {
		packer.pack(wd::command::SUCCESS);
		packer.pack(id);
	});
}

void
pinger::err(int id, int request, int status)
{
	wd::response(wd::command::CONTROL, request, [&](auto& packer) {
		packer.pack(status);
		packer.pack(id);
	});

	std::unique_lock<std::shared_timed_mutex> lock(_mutex);
	_map.erase(id);
}

void
pinger::create(wd::receiver& recv, int request)
{
	auto id       = ++_id;
	auto target   = recv.next().as<std::string>();
	auto interval = recv.next().as<uint32_t>();

	if (interval == 0) {
		wd::response(wd::command::CONTROL, request, [=](auto& packer) {
			packer.pack(wd::command::pinger::error::INVALID_INTERVAL);
			packer.pack(id);
		});

		return;
	}

	std::unique_lock<std::shared_timed_mutex> lock(_mutex);
	_map.emplace(std::piecewise_construct,
		std::make_tuple(id),
		std::make_tuple(this, id, request, wd::pinger::settings {
			.target   = target,
			.interval = interval
		}));
}

void
pinger::start(wd::receiver& recv, int request, int id)
{
	std::shared_lock<std::shared_timed_mutex> lock(_mutex);
	_map.at(id).start(request);
}

void
pinger::stop(wd::receiver& recv, int request, int id)
{
	std::shared_lock<std::shared_timed_mutex> lock(_mutex);
	_map.at(id).stop(request);
}

void
pinger::destroy(wd::receiver& recv, int request, int id)
{
	std::unique_lock<std::shared_timed_mutex> lock(_mutex);
	_map.at(id).destroy(request);
	_map.erase(id);
}

namespace wd {
	std::unique_ptr<module>
	make_pinger()
	{
		return std::unique_ptr<module>(new ::pinger());
	}
}
