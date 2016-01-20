#include <wd/packet>

#include <string>
#include <sstream>

namespace wd {
	namespace packet {
		static
		std::string
		_ip(const in_addr* addr)
		{
			std::ostringstream ss;

			ss << ( addr->s_addr        & 0xff) << '.';
			ss << ((addr->s_addr >> 8)  & 0xff) << '.';
			ss << ((addr->s_addr >> 16) & 0xff) << '.';
			ss << ((addr->s_addr >> 24) & 0xff);

			return ss.str();
		}

		void
		pack(msgpack::packer<std::ostream>& packer, const header* header, size_t offset, const ip* packet)
		{
			auto OFFSET = offset + packet->header * 4;

			packer.pack("ip");
			packer.pack_map(9);

			packer.pack("dscp");
			packer.pack_uint8(packet->dscp);

			packer.pack("ecn");
			packer.pack_uint8(packet->ecn);

			packer.pack("length");
			packer.pack_uint16(ntohl(packet->length));

			packer.pack("id");
			packer.pack_uint16(ntohl(packet->id));

			packer.pack("ttl");
			packer.pack_uint8(packet->ttl);

			packer.pack("protocol");
			packer.pack_uint8(packet->protocol);

			packer.pack("checksum");
			packer.pack_uint16(ntohs(packet->checksum));

			packer.pack("source");
			packer.pack(_ip(&packet->source));

			packer.pack("destination");
			packer.pack(_ip(&packet->destination));

			if (packet->header > 5) {
				// TODO: decode options
			}

			switch (packet->protocol) {
				default:
					pack(packer, header, OFFSET, reinterpret_cast<const unknown*>(packet) + OFFSET);
			}
		}
	}
}
