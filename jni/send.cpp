#include <mutex>

#include <msgpack.hpp>
#include <wd/send>

namespace wd {
	std::mutex locker;

	void
	send(std::function<void(msgpack::packer<std::ostream>&)> body)
	{
		locker.lock();

		msgpack::packer<std::ostream> packer(std::cout);
		body(packer);

		std::cout.flush();
		locker.unlock();
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
