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
			return ntohs(packet->checksum);
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

		std::optional<std::string>
		to_string(enum icmp::type type)
		{
			switch (type) {
				case icmp::ECHO_REPLY:
					return std::string("echo-reply");

				case icmp::DESTINATION_UNREACHABLE:
					return std::string("destination-unreachable");

				case icmp::SOURCE_QUENCH:
					return std::string("source-quench");

				case icmp::REDIRECT_MESSAGE:
					return std::string("redirect-message");

				case icmp::ECHO_REQUEST:
					return std::string("echo-request");

				case icmp::ROUTER_ADVERTISEMENT:
					return std::string("router-advertisement");

				case icmp::ROUTER_SOLICITATION:
					return std::string("router-solicitation");

				case icmp::TIME_EXCEEDED:
					return std::string("time-exceeded");

				case icmp::PARAMETER_PROBLEM:
					return std::string("parameter-problem");

				case icmp::TIMESTAMP_REQUEST:
					return std::string("timestamp-request");

				case icmp::TIMESTAMP_REPLY:
					return std::string("timestamp-reply");

				case icmp::INFORMATION_REQUEST:
					return std::string("information-request");

				case icmp::INFORMATION_REPLY:
					return std::string("information-reply");

				case icmp::ADDRESS_MASK_REQUEST:
					return std::string("address-mask-request");

				case icmp::ADDRESS_MASK_REPLY:
					return std::string("address-mask-reply");

				case icmp::TRACEROUTE:
					return std::string("traceroute");

				default:
					return std::nullopt;
			}
		}

		std::optional<std::string>
		to_string(enum icmp::code::destination_unreachable code)
		{
			switch (code) {
				case icmp::code::DESTINATION_NETWORK_UNREACHABLE:
					return std::string("destination-network-unreachable");

				case icmp::code::DESTINATION_HOST_UNREACHABLE:
					return std::string("destination-host-unreachable");

				case icmp::code::DESTINATION_PROTOCOL_UNREACHABLE:
					return std::string("destination-protocol-unreachable");

				case icmp::code::DESTINATION_PORT_UNREACHABLE:
					return std::string("destination-port-unreachable");

				case icmp::code::FRAGMENTATION_REQUIRED:
					return std::string("fragmentation-required");

				case icmp::code::SOURCE_ROUTE_FAILED:
					return std::string("source-route-failed");

				case icmp::code::DESTINATION_NETWORK_UNKNOWN:
					return std::string("destination-network-unknown");

				case icmp::code::DESTINATION_HOST_UNKNOWN:
					return std::string("destination-host-unknown");

				case icmp::code::SOURCE_HOST_ISOLATED:
					return std::string("source-host-isolated");

				case icmp::code::NETWORK_ADMINISTRATIVELY_PROHIBITED:
					return std::string("network-administratively-prohibited");

				case icmp::code::HOST_ADMINISTRATIVELY_PROHIBITED:
					return std::string("host-administratively-prohibited");

				case icmp::code::NETWORK_UNREACHABLE_FOR_TOS:
					return std::string("network-unreachable-for-tos");

				case icmp::code::HOST_UNREACHABLE_FOR_TOS:
					return std::string("host-unreachable-for-tos");

				case icmp::code::COMMUNICATION_ADMINISTRATIVELY_PROHIBITED:
					return std::string("communication-administratively-prohibited");

				case icmp::code::HOST_PRECEDENCE_VIOLATION:
					return std::string("host-precedence-violation");

				case icmp::code::PRECEDENT_CUTOFF_IN_EFFECT:
					return std::string("precedent-cutoff-in-effect");

				default:
					return std::nullopt;
			}
		}

		std::optional<std::string>
		to_string(enum icmp::code::redirect_message code)
		{
			switch (code) {
				case icmp::code::REDIRECT_DATAGRAM_FOR_NETWORK:
					return std::string("redirect-datagram-for-network");

				case icmp::code::REDIRECT_DATAGRAM_FOR_HOST:
					return std::string("redirect-datagram-for-host");

				case icmp::code::REDIRECT_DATAGRAM_FOR_TOS_AND_NETWORK:
					return std::string("redirect-datagram-for-tos-and-network");

				case icmp::code::REDIRECT_DATAGRAM_FOR_TOS_AND_HOST:
					return std::string("redirect-datagram-for-tos-and-host");

				default:
					return std::nullopt;
			}
		}

		std::optional<std::string>
		to_string(enum icmp::code::parameter_problem code)
		{
			switch (code) {
				case icmp::code::POINTER_INDICATES_ERROR:
					return std::string("pointer-indicates-error");

				case icmp::code::MISSING_REQUIRED_OPTION:
					return std::string("missing-required-option");

				case icmp::code::BAD_LENGTH:
					return std::string("bad-length");

				default:
					return std::nullopt;
			}
		}

		size_t
		icmp::pack(msgpack::packer<std::ostream>& packer, const packet::header* header, const ether::raw* ether, const ip::raw* ip)
		{
			auto OFFSET = reinterpret_cast<const char*>(packet) - reinterpret_cast<const char*>(ether);
			auto LENGTH = sizeof(icmp::raw);

			packer.pack("icmp");
			packer.pack_map(4);

			packer.pack("type");
			if (auto string = to_string(type())) {
				packer.pack(*string);
			}
			else {
				packer.pack_uint8(packet->type);
			}

			packer.pack("code");
			switch (type()) {
				case icmp::DESTINATION_UNREACHABLE: {
					if (auto string = to_string(code<icmp::code::destination_unreachable>())) {
						packer.pack(*string);
					}
					else {
						packer.pack_uint8(packet->code);
					}

					break;
				}

				case icmp::REDIRECT_MESSAGE: {
					if (auto string = to_string(code<icmp::code::redirect_message>())) {
						packer.pack(*string);
					}
					else {
						packer.pack_uint8(packet->code);
					}

					break;
				}

				case icmp::PARAMETER_PROBLEM: {
					if (auto string = to_string(code<icmp::code::parameter_problem>())) {
						packer.pack(*string);
					}
					else {
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
