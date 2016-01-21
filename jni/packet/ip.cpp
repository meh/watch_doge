#include <wd/packet>
#include <wd/log>

#include <string>
#include <sstream>

namespace wd {
	namespace packet {
		std::string
		ip::address(const in_addr* addr)
		{
			std::ostringstream ss;

			ss << ( addr->s_addr        & 0xff) << '.';
			ss << ((addr->s_addr >> 8)  & 0xff) << '.';
			ss << ((addr->s_addr >> 16) & 0xff) << '.';
			ss << ((addr->s_addr >> 24) & 0xff);

			return ss.str();
		}

		size_t
		ip::pack(msgpack::packer<std::ostream>& packer, const ip* ip)
		{
			auto LENGTH = ip->header * 4;

			packer.pack_map(10);

			packer.pack("dscp");
			packer.pack_uint8(ip->dscp);

			packer.pack("ecn");
			packer.pack_uint8(ip->ecn);

			packer.pack("length");
			packer.pack_uint16(ntohl(ip->length));

			packer.pack("id");
			packer.pack_uint16(ntohl(ip->id));

			packer.pack("ttl");
			packer.pack_uint8(ip->ttl);

			packer.pack("checksum");
			packer.pack_uint16(ntohs(ip->checksum));

			packer.pack("source");
			packer.pack(ip::address(&ip->source));

			packer.pack("destination");
			packer.pack(ip::address(&ip->destination));

			packer.pack("options");
			if (ip->header > 5) {
				// TODO: actually decode options
				packer.pack_array(0);
			}
			else {
				packer.pack_array(0);
			}

			packer.pack("protocol");
			switch (ip->protocol) {
				case ip::ICMP: packer.pack("icmp");
					break;

				default:
					packer.pack_uint8(ip->protocol);
			}

			return LENGTH;
		}

		size_t
		ip::pack(msgpack::packer<std::ostream>& packer, const packet::header* header, const ether* ether, const ip* ip)
		{
			packer.pack("ip");

			auto OFFSET = reinterpret_cast<const char*>(ip) - reinterpret_cast<const char*>(ether);
			auto LENGTH = pack(packer, ip);

			switch (ip->protocol) {
				case ip::ICMP: {
					ip::icmp::pack(packer, header, ether, ip, reinterpret_cast<const ip::icmp*>(
						reinterpret_cast<const char*>(ip) + LENGTH));

					break;
				}

				default:
					unknown(packer, header, OFFSET, reinterpret_cast<const char*>(ip) + LENGTH);
			}

			return LENGTH;
		}
	}
}
