#include <packet/unknown>

namespace wd {
	namespace packet {
		size_t
		unknown::pack(msgpack::packer<std::ostream>& packer)
		{
			auto LENGTH = packet->caplen - offset;

			packer.pack_bin(LENGTH);
			packer.pack_bin_body(data, LENGTH);

			return LENGTH;
		}
	}
}
