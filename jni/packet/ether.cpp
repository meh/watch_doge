#include <wd/packet>
#include <wd/log>

#include <string>
#include <sstream>

namespace wd {
	namespace packet {
		std::string
		ether::address(const uint8_t* addr)
		{
			std::ostringstream ss;

			ss << std::hex << std::setfill('0');

			for (int i = 1; i < ETHER_ADDR_LEN; i++) {
				ss << std::setw(2) << static_cast<unsigned>(addr[i - 1]) << ':';
			}

			ss << std::setw(2) << static_cast<unsigned>(addr[ETHER_ADDR_LEN - 1]);

			return ss.str();
		};

		size_t
		ether::pack(msgpack::packer<std::ostream>& packer, const ether* packet)
		{
			auto LENGTH = sizeof(ether);

			packer.pack_map(3);

			packer.pack("source");
			packer.pack(ether::address(packet->source));

			packer.pack("destination");
			packer.pack(ether::address(packet->destination));

			packer.pack("type");
			switch (ntohs(packet->type)) {
				case ether::IPv4: packer.pack("ip");
					break;

				default:
					packer.pack_uint16(ntohs(packet->type));
			}

			return LENGTH;
		}

		size_t
		ether::pack(msgpack::packer<std::ostream>& packer, const header* header, const ether* packet)
		{
			packer.pack("ether");

			auto LENGTH = ether::pack(packer, packet);

			switch (ntohs(packet->type)) {
				case ether::IPv4:
					ip::pack(packer, header, packet, reinterpret_cast<const ip*>(
						reinterpret_cast<const char*>(packet) + LENGTH));

					break;

				default:
					unknown(packer, header, LENGTH, reinterpret_cast<const char*>(packet) + LENGTH);
			}

			return LENGTH;
		}
	}
}
