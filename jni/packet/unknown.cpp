#include <wd/packet>

namespace wd {
	namespace packet {
		void
		pack(msgpack::packer<std::ostream>& packer, const header* header, size_t offset, const unknown* packet)
		{
			packer.pack_bin(header->len - offset);
			packer.pack_bin_body(packet, header->len - offset);
		}
	}
}
