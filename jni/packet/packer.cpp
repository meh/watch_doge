#include <wd/common>
#include <wd/packet/packer>
#include <wd/packet/ether>

namespace wd {
	namespace packet {
		void
		pack(msgpack::packer<std::ostream>& packer, int32_t pid, const header* header, const uint8_t* packet)
		{
			// packet id
			packer.pack_uint32(pid);

			// packet size
			packer.pack_uint32(header->len);
			packer.pack_uint32(header->caplen);

			// packet timestamp
			packer.pack_int64(header->ts.tv_sec);
			packer.pack_int64(header->ts.tv_usec);

			// start from ethernet, it does the rest itself
			packet::ether ether(reinterpret_cast<const packet::ether::raw*>(packet));
			ether.pack(packer, header);

			// no more decoded data
			packer.pack_nil();
		}
	}
}
