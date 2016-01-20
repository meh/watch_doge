#include <wd/packet>

#include <string>
#include <sstream>

namespace wd {
	namespace packet {
		static
		std::string
		_mac(const uint8_t* addr)
		{
			std::ostringstream ss;

			ss << std::hex << std::setfill('0');

			for (int i = 1; i < ETHER_ADDR_LEN; i++) {
				ss << std::setw(2) << static_cast<unsigned>(addr[i - 1]) << ':';
			}

			ss << std::setw(2) << static_cast<unsigned>(addr[ETHER_ADDR_LEN - 1]);

			return ss.str();
		};

		void
		pack(msgpack::packer<std::ostream>& packer, const header* header, size_t offset, const ethernet* packet)
		{
			auto OFFSET = offset + sizeof(ethernet);

			packer.pack("ether");
			packer.pack_map(3);

			packer.pack("type");
			packer.pack_uint16(ntohs(packet->type));

			packer.pack("source");
			packer.pack(_mac(packet->source));

			packer.pack("destination");
			packer.pack(_mac(packet->destination));

			switch (ntohs(packet->type)) {
				case ethernet::IPv4: {
					pack(packer, header, OFFSET, reinterpret_cast<const ip*>(
						reinterpret_cast<const char*>(packet) + OFFSET));

					break;
				}

				default:
					pack(packer, header, OFFSET, reinterpret_cast<const unknown*>(packet) + OFFSET);
			}
		}
	}
}
