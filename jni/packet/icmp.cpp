#include <packet/unknown>
#include <packet/ether>
#include <packet/ip>
#include <packet/icmp>

namespace wd {
	namespace packet {
		uint16_t
		icmp::echo::identifier() const
		{
			return ntohs(packet->identifier);
		}

		uint16_t
		icmp::echo::sequence() const
		{
			return ntohs(packet->sequence);
		}

		const char*
		icmp::echo::data() const
		{
			return packet->data;
		}

		size_t
		icmp::echo::length(const header* header, size_t offset) const
		{
			return header->caplen - (sizeof(icmp::raw) + sizeof(icmp::echo) + offset);
		}

		uint16_t
		icmp::timestamp::identifier() const
		{
			return ntohs(packet->identifier);
		}

		uint16_t
		icmp::timestamp::sequence() const
		{
			return ntohs(packet->sequence);
		}

		uint32_t
		icmp::timestamp::originate() const
		{
			return ntohl(packet->originate);
		}

		uint32_t
		icmp::timestamp::receive() const
		{
			return ntohl(packet->receive);
		}

		uint32_t
		icmp::timestamp::transmit() const
		{
			return ntohl(packet->transmit);
		}

		uint16_t
		icmp::information::identifier() const
		{
			return ntohs(packet->identifier);
		}

		uint16_t
		icmp::information::sequence() const
		{
			return ntohs(packet->sequence);
		}

		uint8_t
		icmp::parameter_problem::pointer() const
		{
			return packet->pointer;
		}

		ip
		icmp::parameter_problem::header() const
		{
			return reinterpret_cast<const ip::raw*>(packet->payload);
		}

		const char*
		icmp::parameter_problem::data() const
		{
			return packet->payload + (header().header() * 4);
		}

		std::string
		icmp::redirect_message::gateway() const
		{
			return ip::address(&packet->gateway);
		}

		ip
		icmp::redirect_message::header() const
		{
			return reinterpret_cast<const ip::raw*>(packet->payload);
		}

		const char*
		icmp::redirect_message::data() const
		{
			return packet->payload + (header().header() * 4);
		}

		ip
		icmp::previous::header() const
		{
			return reinterpret_cast<const ip::raw*>(packet->payload);
		}

		const char*
		icmp::previous::data() const
		{
			return packet->payload + (header().header() * 4);
		}

		enum icmp::type
		icmp::type() const
		{
			return static_cast<enum icmp::type>(packet->type);
		}

		template<>
		icmp::code::destination_unreachable
		icmp::code() const
		{
			if (type() != icmp::DESTINATION_UNREACHABLE) {
				throw 0xBADB0117;
			}

			return static_cast<enum icmp::code::destination_unreachable>(packet->code);
		}

		template<>
		icmp::code::redirect_message
		icmp::code() const
		{
			if (type() != icmp::REDIRECT_MESSAGE) {
				throw 0xBADB0117;
			}

			return static_cast<enum icmp::code::redirect_message>(packet->code);
		}

		template<>
		icmp::code::parameter_problem
		icmp::code() const
		{
			if (type() != icmp::PARAMETER_PROBLEM) {
				throw 0xBADB0117;
			}

			return static_cast<enum icmp::code::parameter_problem>(packet->code);
		}

		uint16_t
		icmp::checksum() const
		{
			return ntohl(packet->checksum);
		}

		template<>
		icmp::echo
		icmp::details() const
		{
			if (type() != icmp::ECHO_REQUEST && type() != icmp::ECHO_REPLY) {
				throw 0xBADB0117;
			}

			return reinterpret_cast<const icmp::echo::raw*>(
				reinterpret_cast<const char*>(packet) + sizeof(icmp::raw));
		}

		template<>
		icmp::timestamp
		icmp::details() const
		{
			if (type() != icmp::TIMESTAMP_REQUEST && type() != icmp::TIMESTAMP_REPLY) {
				throw 0xBADB0117;
			}

			return reinterpret_cast<const icmp::timestamp::raw*>(
				reinterpret_cast<const char*>(packet) + sizeof(icmp::raw));
		}

		template<>
		icmp::information
		icmp::details() const
		{
			if (type() != icmp::INFORMATION_REQUEST && type() != icmp::INFORMATION_REPLY) {
				throw 0xBADB0117;
			}

			return reinterpret_cast<const icmp::information::raw*>(
				reinterpret_cast<const char*>(packet) + sizeof(icmp::raw));
		}

		template<>
		icmp::parameter_problem
		icmp::details() const
		{
			if (type() != icmp::PARAMETER_PROBLEM) {
				throw 0xBADB0117;
			}

			return reinterpret_cast<const icmp::parameter_problem::raw*>(
				reinterpret_cast<const char*>(packet) + sizeof(icmp::raw));
		}

		template<>
		icmp::redirect_message
		icmp::details() const
		{
			if (type() != icmp::REDIRECT_MESSAGE) {
				throw 0xBADB0117;
			}

			return reinterpret_cast<const icmp::redirect_message::raw*>(
				reinterpret_cast<const char*>(packet) + sizeof(icmp::raw));
		}

		template<>
		icmp::previous
		icmp::details() const
		{
			if (type() != icmp::SOURCE_QUENCH && type() != icmp::DESTINATION_UNREACHABLE && type() != TIME_EXCEEDED) {
				throw 0xBADB0117;
			}

			return reinterpret_cast<const icmp::previous::raw*>(
				reinterpret_cast<const char*>(packet) + sizeof(icmp::raw));
		}

		size_t
		icmp::pack(msgpack::packer<std::ostream>& packer, const packet::header* header, const ether::raw* ether, const ip::raw* ip)
		{
			auto OFFSET = reinterpret_cast<const char*>(packet) - reinterpret_cast<const char*>(ether);
			auto LENGTH = sizeof(icmp::raw);

			packer.pack("icmp");
			packer.pack_map(4);

			packer.pack("type");
			switch (type()) {
				case icmp::ECHO_REPLY:
					packer.pack("echo-reply");
					break;

				case icmp::DESTINATION_UNREACHABLE:
					packer.pack("destination-unreachable");
					break;

				case icmp::SOURCE_QUENCH:
					packer.pack("source-quench");
					break;

				case icmp::REDIRECT_MESSAGE:
					packer.pack("redirect-message");
					break;

				case icmp::ECHO_REQUEST:
					packer.pack("echo-request");
					break;

				case icmp::ROUTER_ADVERTISEMENT:
					packer.pack("router-advertisement");
					break;

				case icmp::ROUTER_SOLICITATION:
					packer.pack("router-solicitation");
					break;

				case icmp::TIME_EXCEEDED:
					packer.pack("time-exceeded");
					break;

				case icmp::PARAMETER_PROBLEM:
					packer.pack("parameter-problem");
					break;

				case icmp::TIMESTAMP_REQUEST:
					packer.pack("timestamp-request");
					break;

				case icmp::TIMESTAMP_REPLY:
					packer.pack("timestamp-reply");
					break;

				case icmp::INFORMATION_REQUEST:
					packer.pack("information-request");
					break;

				case icmp::INFORMATION_REPLY:
					packer.pack("information-reply");
					break;

				case icmp::ADDRESS_MASK_REQUEST:
					packer.pack("address-mask-request");
					break;

				case icmp::ADDRESS_MASK_REPLY:
					packer.pack("address-mask-reply");
					break;

				case icmp::TRACEROUTE:
					packer.pack("traceroute");
					break;

				default:
					packer.pack_uint8(packet->type);
			}

			packer.pack("code");
			switch (type()) {
				case icmp::DESTINATION_UNREACHABLE: {
					switch (code<icmp::code::destination_unreachable>()) {
						case icmp::code::DESTINATION_NETWORK_UNREACHABLE:
							packer.pack("destination-network-unreachable");
							break;

						case icmp::code::DESTINATION_HOST_UNREACHABLE:
							packer.pack("destination-host-unreachable");
							break;

						case icmp::code::DESTINATION_PROTOCOL_UNREACHABLE:
							packer.pack("destination-protocol-unreachable");
							break;

						case icmp::code::DESTINATION_PORT_UNREACHABLE:
							packer.pack("destination-port-unreachable");
							break;

						case icmp::code::FRAGMENTATION_REQUIRED:
							packer.pack("fragmentation-required");
							break;

						case icmp::code::SOURCE_ROUTE_FAILED:
							packer.pack("source-route-failed");
							break;

						case icmp::code::DESTINATION_NETWORK_UNKNOWN:
							packer.pack("destination-network-unknown");
							break;

						case icmp::code::DESTINATION_HOST_UNKNOWN:
							packer.pack("destination-host-unknown");
							break;

						case icmp::code::SOURCE_HOST_ISOLATED:
							packer.pack("source-host-isolated");
							break;

						case icmp::code::NETWORK_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("network-administratively-prohibited");
							break;

						case icmp::code::HOST_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("host-administratively-prohibited");
							break;

						case icmp::code::NETWORK_UNREACHABLE_FOR_TOS:
							packer.pack("network-unreachable-for-tos");
							break;

						case icmp::code::HOST_UNREACHABLE_FOR_TOS:
							packer.pack("host-unreachable-for-tos");
							break;

						case icmp::code::COMMUNICATION_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("communication-administratively-prohibited");
							break;

						case icmp::code::HOST_PRECEDENCE_VIOLATION:
							packer.pack("host-precedence-violation");
							break;

						case icmp::code::PRECEDENT_CUTOFF_IN_EFFECT:
							packer.pack("precedent-cutoff-in-effect");
							break;

						default:
							packer.pack_uint8(packet->code);
					}

					break;
				}

				case icmp::REDIRECT_MESSAGE: {
					switch (code<icmp::code::redirect_message>()) {
						case icmp::code::REDIRECT_DATAGRAM_FOR_NETWORK:
							packer.pack("redirect-datagram-for-network");
							break;

						case icmp::code::REDIRECT_DATAGRAM_FOR_HOST:
							packer.pack("redirect-datagram-for-host");
							break;

						case icmp::code::REDIRECT_DATAGRAM_FOR_TOS_AND_NETWORK:
							packer.pack("redirect-datagram-for-tos-and-network");
							break;

						case icmp::code::REDIRECT_DATAGRAM_FOR_TOS_AND_HOST:
							packer.pack("redirect-datagram-for-tos-and-host");
							break;

						default:
							packer.pack_uint8(packet->code);
					}

					break;
				}

				case icmp::PARAMETER_PROBLEM: {
					switch (code<icmp::code::parameter_problem>()) {
						case icmp::code::POINTER_INDICATES_ERROR:
							packer.pack("pointer-indicates-error");
							break;

						case icmp::code::MISSING_REQUIRED_OPTION:
							packer.pack("missing-required-option");
							break;

						case icmp::code::BAD_LENGTH:
							packer.pack("bad-length");
							break;

						default:
							packer.pack_uint8(packet->code);
					}

					break;
				}

				default:
					packer.pack_uint8(packet->code);
			}

			packer.pack("checksum");
			packer.pack_uint16(checksum());

			packer.pack("details");
			switch (type()) {
				case icmp::ECHO_REQUEST:
				case icmp::ECHO_REPLY: {
					auto echo = details<icmp::echo>();

					packer.pack_map(3);

					packer.pack("identifier");
					packer.pack_uint16(echo.identifier());

					packer.pack("sequence");
					packer.pack_uint16(echo.sequence());

					auto length = echo.length(header, OFFSET);

					packer.pack("data");
					packer.pack_bin(length);
					packer.pack_bin_body(echo.data(), length);

					LENGTH += sizeof(icmp::echo::raw) + length;

					break;
				}

				case icmp::TIMESTAMP_REQUEST:
				case icmp::TIMESTAMP_REPLY: {
					auto timestamp = details<icmp::timestamp>();

					packer.pack_map(5);

					packer.pack("identifier");
					packer.pack_uint16(timestamp.identifier());

					packer.pack("sequence");
					packer.pack_uint16(timestamp.sequence());

					packer.pack("originate");
					packer.pack_uint32(timestamp.originate());

					packer.pack("receive");
					packer.pack_uint32(timestamp.receive());

					packer.pack("transmit");
					packer.pack_uint32(timestamp.transmit());

					LENGTH += sizeof(icmp::timestamp::raw);

					break;
				}

				case icmp::INFORMATION_REQUEST:
				case icmp::INFORMATION_REPLY: {
					auto information = details<icmp::information>();

					packer.pack_map(2);

					packer.pack("identifier");
					packer.pack_uint16(information.identifier());

					packer.pack("sequence");
					packer.pack_uint16(information.sequence());

					LENGTH += sizeof(icmp::information::raw);

					break;
				}

				case icmp::PARAMETER_PROBLEM: {
					auto parameter_problem = details<icmp::parameter_problem>();

					packer.pack_map(2);

					packer.pack("pointer");
					packer.pack_uint8(parameter_problem.pointer());

					packer.pack("header");
					auto offset = parameter_problem.header().pack(packer);

					packer.pack("data");
					packer.pack_bin(8);
					packer.pack_bin_body(parameter_problem.data(), 8);

					LENGTH += sizeof(icmp::parameter_problem::raw) + offset + 8;

					break;
				}

				case icmp::REDIRECT_MESSAGE: {
					auto redirect_message = details<icmp::redirect_message>();

					packer.pack_map(2);

					packer.pack("gateway");
					packer.pack(redirect_message.gateway());

					packer.pack("header");
					auto offset = redirect_message.header().pack(packer);

					packer.pack("data");
					packer.pack_bin(8);
					packer.pack_bin_body(redirect_message.data(), 8);

					LENGTH += sizeof(icmp::redirect_message::raw) + offset + 8;

					break;
				}

				case icmp::SOURCE_QUENCH:
				case icmp::DESTINATION_UNREACHABLE:
				case icmp::TIME_EXCEEDED: {
					auto previous = details<icmp::previous>();

					packer.pack_map(2);

					packer.pack("header");
					auto offset = previous.header().pack(packer);

					packer.pack("data");
					packer.pack_bin(8);
					packer.pack_bin_body(previous.data(), 8);

					LENGTH += sizeof(icmp::previous::raw) + offset + 8;

					break;
				}
			}

			if (header->caplen > OFFSET + LENGTH) {
				unknown unknown(header, OFFSET + LENGTH, reinterpret_cast<const char*>(packet) + LENGTH);
				unknown.pack(packer);
			}
		}
	}
}
