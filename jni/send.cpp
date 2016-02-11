#include <wd/common>

namespace wd {
	std::mutex locker;

	void
	send(std::function<void(msgpack::packer<std::ostream>&)> body)
	{
		std::lock_guard<std::mutex> guard(locker);

		msgpack::packer<std::ostream> packer(std::cout);
		body(packer);

		std::cout.flush();
	}

	void
	response(int type, int request, std::function<void(msgpack::packer<std::ostream>&)> body)
	{
		send([&](auto& packer) {
			packer.pack_int(type);
			packer.pack_int(request);
			body(packer);
		});
	}
}
