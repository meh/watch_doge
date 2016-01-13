#include <mutex>

#include <msgpack.hpp>
#include <wd/send>

namespace wd {
	std::mutex locker;

	void
	send(int id, std::function<void(msgpack::packer<std::ostream>&)> body)
	{
		locker.lock();

		msgpack::packer<std::ostream> packer(std::cout);
		packer.pack_int(id);
		body(packer);

		std::cout.flush();
		locker.unlock();
	}
}
