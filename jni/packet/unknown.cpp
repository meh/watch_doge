#include <wd/packet>

namespace wd {
	namespace packet {
		size_t
		unknown(msgpack::packer<std::ostream>& packer, const header* header, size_t offset, const char* data)
		{
			auto LENGTH = header->len - offset;

			packer.pack_bin(LENGTH);
			packer.pack_bin_body(data, LENGTH);

			return LENGTH;
		}
	}
}
