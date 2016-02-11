#include <packet/unknown>
#include <packet/ether>
#include <packet/ip>
#include <packet/icmp>
#include <packet/tcp>

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

		uint8_t
		ip::version() const
		{
			return packet->version_and_header >> 4;
		}

		uint8_t
		ip::header() const
		{
			return packet->version_and_header & 0xf;
		}

		uint8_t
		ip::dscp() const
		{
			return packet->dscp_and_ecn >> 2;
		}

		uint8_t
		ip::ecn() const
		{
			return packet->dscp_and_ecn & 0x3;
		}

		uint16_t
		ip::length() const
		{
			return ntohs(packet->length);
		}

		uint16_t
		ip::id() const
		{
			return ntohs(packet->id);
		}

		uint16_t
		ip::flags() const
		{
			return ntohs(packet->flags_and_offset) >> 13;
		}

		uint16_t
		ip::offset() const
		{
			return ntohs(packet->flags_and_offset) & 0x1fff;
		}

		uint8_t
		ip::ttl() const
		{
			return packet->ttl;
		}

		enum ip::protocol
		ip::protocol() const
		{
			return static_cast<enum ip::protocol>(packet->protocol);
		}

		uint16_t
		ip::checksum() const
		{
			return packet->checksum;
		}

		std::string
		ip::source() const
		{
			return ip::address(&packet->source);
		}

		std::string
		ip::destination() const
		{
			return ip::address(&packet->destination);
		}

		size_t
		ip::pack(msgpack::packer<std::ostream>& packer)
		{
			auto LENGTH = header() * 4;

			packer.pack_map(12);

			packer.pack("version");
			packer.pack_uint8(version());

			packer.pack("header");
			packer.pack_uint8(header());

			packer.pack("dscp");
			packer.pack_uint8(dscp());

			packer.pack("ecn");
			packer.pack_uint8(ecn());

			packer.pack("length");
			packer.pack_uint16(length());

			packer.pack("id");
			packer.pack_uint16(id());

			packer.pack("ttl");
			packer.pack_uint8(ttl());

			packer.pack("checksum");
			packer.pack_uint16(checksum());

			packer.pack("source");
			packer.pack(source());

			packer.pack("destination");
			packer.pack(destination());

			packer.pack("options");
			if (header() > 5) {
				// TODO: actually decode options
				packer.pack_array(0);
			}
			else {
				packer.pack_array(0);
			}

			packer.pack("protocol");
			switch (protocol()) {
				case ip::ICMP:
					packer.pack("icmp");
					break;

				case ip::TCP:
					packer.pack("tcp");
					break;

				default:
					packer.pack_uint8(protocol());
			}

			return LENGTH;
		}

		size_t
		ip::pack(msgpack::packer<std::ostream>& packer, const packet::header* header, const ether::raw* ether)
		{
			packer.pack("ip");

			auto OFFSET = reinterpret_cast<const char*>(packet) - reinterpret_cast<const char*>(ether);
			auto LENGTH = pack(packer);

			switch (protocol()) {
				case ip::ICMP: {
					icmp icmp(reinterpret_cast<const icmp::raw*>(reinterpret_cast<const char*>(packet) + LENGTH));
					icmp.pack(packer, header, ether, packet);

					break;
				}

				case ip::TCP: {
					tcp tcp(reinterpret_cast<const tcp::raw*>(reinterpret_cast<const char*>(packet) + LENGTH));
					tcp.pack(packer, header, ether, packet);

					break;
				}

				default: {
					unknown unknown(header, OFFSET, reinterpret_cast<const char*>(packet) + LENGTH);
					unknown.pack(packer);
				}
			}

			return LENGTH;
		}
	}
}
