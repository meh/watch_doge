#include <wd/packet>
#include <wd/log>

#include <string>
#include <sstream>

namespace wd {
	namespace packet {
		static
		std::string
		address(const uint8_t* addr)
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
		pack(msgpack::packer<std::ostream>& packer, const header* header, const ether* packet)
		{
			auto OFFSET = sizeof(ether);

			packer.pack("ether");
			packer.pack_map(3);

			packer.pack("source");
			packer.pack(address(packet->source));

			packer.pack("destination");
			packer.pack(address(packet->destination));

			packer.pack("type");
			switch (ntohs(packet->type)) {
				case ether::IPv4: {
					packer.pack("ip");
					pack(packer, header, packet, reinterpret_cast<const ip*>(
						reinterpret_cast<const char*>(packet) + OFFSET));

					break;
				}

				default: {
					packer.pack_uint16(ntohs(packet->type));
					pack(packer, header, OFFSET, reinterpret_cast<const unknown*>(
						reinterpret_cast<const char*>(packet) + OFFSET));

					break;
				}
			}
		}
	}
}
