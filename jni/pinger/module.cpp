#include <wd/common>
#include "pinger"

class pinger: public wd::module {
	public:
		void handle(wd::receiver& recv, int request, int command);

		void create(wd::receiver& recv, int request);

	private:
		std::map<int, wd::pinger> _map;
		int                _id;
};

void
pinger::handle(wd::receiver& recv, int request, int command)
{
	switch (command) {
		default:
			wd::response(wd::command::CONTROL, request, [](auto& packer) {
				packer.pack(wd::command::UNKNOWN);
			});
	}
}

void
pinger::create(wd::receiver& recv, int request)
{
	auto id       = ++_id;
	auto target   = recv.next().as<std::string>();
	auto interval = recv.next().as<int64_t>();

	_map.emplace(std::piecewise_construct,
		std::make_tuple(id),
		std::make_tuple(id, target, interval));

	wd::response(wd::command::CONTROL, request, [&](auto& packer) {
		packer.pack(wd::command::SUCCESS);
		packer.pack(id);
	});
}

namespace wd {
	std::unique_ptr<module>
	make_pinger()
	{
		return std::unique_ptr<module>(new ::pinger());
	}
}
