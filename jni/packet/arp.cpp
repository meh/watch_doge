#include <wd/packet/unknown>
#include <wd/packet/ether>
#include <wd/packet/ip>
#include <wd/packet/arp>

namespace wd {
	namespace packet {
		enum arp::hardware
		arp::hardware() const
		{
			return static_cast<enum arp::hardware>(ntohs(packet->hardware_type));
		}

		enum arp::protocol
		arp::protocol() const
		{
			return static_cast<enum arp::protocol>(ntohs(packet->protocol_type));
		}

		enum arp::operation
		arp::operation() const
		{
			return static_cast<enum arp::operation>(ntohs(packet->operation));
		}

		arp::result
		arp::sender() const
		{
			return part(reinterpret_cast<const uint8_t*>(packet->payload));

		}

		arp::result
		arp::target() const
		{
			return part(reinterpret_cast<const uint8_t*>(packet->payload)
				+ packet->hardware_length + packet->protocol_length);
		}

		arp::result
		arp::part(const uint8_t* pointer) const
		{
			arp::result::type hardware;
			arp::result::type protocol;

			switch (this->hardware()) {
				case arp::ETHERNET:
					hardware = ether::address(pointer);
					break;

				default:
					hardware = std::vector<uint8_t>(pointer, pointer + packet->hardware_length);
			}

			pointer += packet->hardware_length;

			switch (this->protocol()) {
				case arp::IPv4:
					protocol = ip::address(reinterpret_cast<const in_addr*>(pointer));
					break;

				default:
					protocol = std::vector<uint8_t>(pointer, pointer + packet->protocol_length);
			}

			return { hardware, protocol };
		}

		size_t
		arp::pack(msgpack::packer<std::ostream>& packer, const header* header, const ether::raw* ether)
		{
			auto OFFSET = reinterpret_cast<const char*>(packet) - reinterpret_cast<const char*>(ether);
			auto LENGTH = sizeof(arp::raw)
				+ (packet->hardware_length * 2)
				+ (packet->protocol_length * 2);

			packer.pack("arp");
			packer.pack_map(5);

			packer.pack("hardware");
			switch (hardware()) {
				case arp::ETHERNET:
					packer.pack("ether");
					break;

				default:
					packer.pack_uint16(hardware());
			}

			packer.pack("protocol");
			switch (protocol()) {
				case arp::IPv4:
					packer.pack("ip");
					break;

				case arp::IPv6:
					packer.pack("ip6");
					break;

				default:
					packer.pack_uint16(protocol());
			}

			packer.pack("operation");
			switch (operation()) {
				case arp::REQUEST:
					packer.pack("request");
					break;

				case arp::REPLY:
					packer.pack("reply");
					break;

				default:
					packer.pack_uint16(operation());
			}

			packer.pack("sender");
			pack(packer, sender());

			packer.pack("target");
			pack(packer, target());

			if (header->len > OFFSET + LENGTH) {
				unknown unknown(header, OFFSET + LENGTH, reinterpret_cast<const char*>(packet) + LENGTH);
				unknown.pack(packer);
			}

			return LENGTH;
		}

		void
		arp::pack(msgpack::packer<std::ostream>& packer, result value)
		{
			packer.pack_map(2);

			packer.pack("hardware");
			pack(packer, value.hardware);

			packer.pack("protocol");
			pack(packer, value.protocol);
		}

		void
		arp::pack(msgpack::packer<std::ostream>& packer, result::type value)
		{
			if (value.is<std::string>()) {
				packer.pack(value.get<std::string>());
			}
			else {
				auto address = value.get<std::vector<uint8_t>>();

				packer.pack_bin(address.size());
				packer.pack_bin_body(reinterpret_cast<const char*>(address.data()), address.size());
			}
		}
	}
}
