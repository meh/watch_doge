#include <wd/packet/unknown>
#include <wd/packet/ether>
#include <wd/packet/arp>
#include <wd/packet/ip>

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

		std::string
		ether::destination() const
		{
			return ether::address(packet->destination);
		}

		std::string
		ether::source() const
		{
			return ether::address(packet->source);
		}

		enum ether::type
		ether::type() const
		{
			return static_cast<enum ether::type>(ntohs(packet->type));
		}

		size_t
		ether::pack(msgpack::packer<std::ostream>& packer)
		{
			packer.pack_map(3);

			packer.pack("source");
			packer.pack(source());

			packer.pack("destination");
			packer.pack(destination());

			packer.pack("type");
			switch (type()) {
				case ether::IPv4:
					packer.pack("ip");
					break;

				case ether::ARP:
					packer.pack("arp");
					break;

				default:
					packer.pack_uint16(ntohs(packet->type));
			}

			return sizeof(ether::raw);
		}

		size_t
		ether::pack(msgpack::packer<std::ostream>& packer, const header* header)
		{
			packer.pack("ether");
			auto LENGTH = pack(packer);

			switch (type()) {
				case ether::IPv4: {
					ip ip(reinterpret_cast<const ip::raw*>(reinterpret_cast<const char*>(packet) + LENGTH));
					ip.pack(packer, header, packet);

					break;
				}

				case ether::ARP: {
					arp arp(reinterpret_cast<const arp::raw*>(reinterpret_cast<const char*>(packet) + LENGTH));
					arp.pack(packer, header, packet);

					break;
				}

				default: {
					unknown unknown(header, LENGTH, reinterpret_cast<const char*>(packet) + LENGTH);
					unknown.pack(packer);
				}
			}

			return LENGTH;
		}
	}
}
