#include <wd/packet>
#include <wd/log>

namespace wd {
	namespace packet {
		size_t
		ip::icmp::pack(msgpack::packer<std::ostream>& packer, const ip::icmp* icmp)
		{
			auto LENGTH = sizeof(ip::icmp);

			packer.pack_map(4);

			packer.pack("type");
			switch (icmp->type) {
				case ip::icmp::ECHO_REPLY:
					packer.pack("echo-reply");
					break;

				case ip::icmp::DESTINATION_UNREACHABLE:
					packer.pack("destination-unreachable");
					break;

				case ip::icmp::SOURCE_QUENCH:
					packer.pack("source-quench");
					break;

				case ip::icmp::REDIRECT_MESSAGE:
					packer.pack("redirect-message");
					break;

				case ip::icmp::ECHO_REQUEST:
					packer.pack("echo-request");
					break;

				case ip::icmp::ROUTER_ADVERTISEMENT:
					packer.pack("router-advertisement");
					break;

				case ip::icmp::ROUTER_SOLICITATION:
					packer.pack("router-solicitation");
					break;

				case ip::icmp::TIME_EXCEEDED:
					packer.pack("time-exceeded");
					break;

				case ip::icmp::PARAMETER_PROBLEM:
					packer.pack("parameter-problem");
					break;

				case ip::icmp::TIMESTAMP_REQUEST:
					packer.pack("timestamp-request");
					break;

				case ip::icmp::TIMESTAMP_REPLY:
					packer.pack("timestamp-reply");
					break;

				case ip::icmp::INFORMATION_REQUEST:
					packer.pack("information-request");
					break;

				case ip::icmp::INFORMATION_REPLY:
					packer.pack("information-reply");
					break;

				case ip::icmp::ADDRESS_MASK_REQUEST:
					packer.pack("address-mask-request");
					break;

				case ip::icmp::ADDRESS_MASK_REPLY:
					packer.pack("address-mask-reply");
					break;

				case ip::icmp::TRACEROUTE:
					packer.pack("traceroute");
					break;

				default:
					packer.pack_uint8(icmp->type);
			}

			packer.pack("code");
			switch (icmp->type) {
				case ip::icmp::DESTINATION_UNREACHABLE: {
					switch (icmp->code.destination_unreachable) {
						case ip::icmp::code::DESTINATION_NETWORK_UNREACHABLE:
							packer.pack("destination-network-unreachable");
							break;

						case ip::icmp::code::DESTINATION_HOST_UNREACHABLE:
							packer.pack("destination-host-unreachable");
							break;

						case ip::icmp::code::DESTINATION_PROTOCOL_UNREACHABLE:
							packer.pack("destination-protocol-unreachable");
							break;

						case ip::icmp::code::DESTINATION_PORT_UNREACHABLE:
							packer.pack("destination-port-unreachable");
							break;

						case ip::icmp::code::FRAGMENTATION_REQUIRED:
							packer.pack("fragmentation-required");
							break;

						case ip::icmp::code::SOURCE_ROUTE_FAILED:
							packer.pack("source-route-failed");
							break;

						case ip::icmp::code::DESTINATION_NETWORK_UNKNOWN:
							packer.pack("destination-network-unknown");
							break;

						case ip::icmp::code::DESTINATION_HOST_UNKNOWN:
							packer.pack("destination-host-unknown");
							break;

						case ip::icmp::code::SOURCE_HOST_ISOLATED:
							packer.pack("source-host-isolated");
							break;

						case ip::icmp::code::NETWORK_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("network-administratively-prohibited");
							break;

						case ip::icmp::code::HOST_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("host-administratively-prohibited");
							break;

						case ip::icmp::code::NETWORK_UNREACHABLE_FOR_TOS:
							packer.pack("network-unreachable-for-tos");
							break;

						case ip::icmp::code::HOST_UNREACHABLE_FOR_TOS:
							packer.pack("host-unreachable-for-tos");
							break;

						case ip::icmp::code::COMMUNICATION_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("communication-administratively-prohibited");
							break;

						case ip::icmp::code::HOST_PRECEDENCE_VIOLATION:
							packer.pack("host-precedence-violation");
							break;

						case ip::icmp::code::PRECEDENT_CUTOFF_IN_EFFECT:
							packer.pack("precedent-cutoff-in-effect");
							break;

						default:
							packer.pack_uint8(icmp->code.raw);
					}

					break;
				}

				case ip::icmp::REDIRECT_MESSAGE: {
					switch (icmp->code.redirect_message) {
						case ip::icmp::code::REDIRECT_DATAGRAM_FOR_NETWORK:
							packer.pack("redirect-datagram-for-network");
							break;

						case ip::icmp::code::REDIRECT_DATAGRAM_FOR_HOST:
							packer.pack("redirect-datagram-for-host");
							break;

						case ip::icmp::code::REDIRECT_DATAGRAM_FOR_TOS_AND_NETWORK:
							packer.pack("redirect-datagram-for-tos-and-network");
							break;

						case ip::icmp::code::REDIRECT_DATAGRAM_FOR_TOS_AND_HOST:
							packer.pack("redirect-datagram-for-tos-and-host");
							break;

						default:
							packer.pack_uint8(icmp->code.raw);
					}

					break;
				}

				case ip::icmp::PARAMETER_PROBLEM: {
					switch (icmp->code.parameter_problem) {
						case ip::icmp::code::POINTER_INDICATES_ERROR:
							packer.pack("pointer-indicates-error");
							break;

						case ip::icmp::code::MISSING_REQUIRED_OPTION:
							packer.pack("missing-required-option");
							break;

						case ip::icmp::code::BAD_LENGTH:
							packer.pack("bad-length");
							break;

						default:
							packer.pack_uint8(icmp->code.raw);
					}

					break;
				}

				default:
					packer.pack_uint8(icmp->code.raw);
			}

			packer.pack("checksum");
			packer.pack_uint16(icmp->checksum);

			return LENGTH;
		}

		size_t
		ip::icmp::pack(msgpack::packer<std::ostream>& packer, const packet::header* header, const ether* ether, const ip* ip, const ip::icmp* icmp)
		{
			packer.pack("icmp");

			auto OFFSET = reinterpret_cast<const char*>(icmp) - reinterpret_cast<const char*>(ether);
			auto LENGTH = pack(packer, icmp);

			packer.pack("details");
			switch (icmp->type) {
				case ip::icmp::ECHO_REQUEST:
				case ip::icmp::ECHO_REPLY: {
					auto echo = reinterpret_cast<const ip::icmp::echo*>(
						reinterpret_cast<const char*>(icmp) + LENGTH);

					packer.pack_map(3);

					packer.pack("identifier");
					packer.pack_uint16(ntohs(echo->identifier));

					packer.pack("sequence");
					packer.pack_uint16(ntohs(echo->sequence));

					auto length = header->len - (OFFSET + LENGTH + sizeof(ip::icmp::echo));

					packer.pack("data");
					packer.pack_bin(length);
					packer.pack_bin_body(echo->data, length);

					LENGTH += sizeof(ip::icmp::echo) + length;

					break;
				}

				case ip::icmp::TIMESTAMP_REQUEST:
				case ip::icmp::TIMESTAMP_REPLY: {
					auto timestamp = reinterpret_cast<const ip::icmp::timestamp*>(
						reinterpret_cast<const char*>(icmp) + LENGTH);

					packer.pack_map(5);

					packer.pack("identifier");
					packer.pack_uint16(ntohs(timestamp->identifier));

					packer.pack("sequence");
					packer.pack_uint16(ntohs(timestamp->sequence));

					packer.pack("originate");
					packer.pack_uint32(ntohl(timestamp->originate));

					packer.pack("receive");
					packer.pack_uint32(ntohl(timestamp->receive));

					packer.pack("transmit");
					packer.pack_uint32(ntohl(timestamp->transmit));

					LENGTH += sizeof(ip::icmp::timestamp);

					break;
				}

				case ip::icmp::INFORMATION_REQUEST:
				case ip::icmp::INFORMATION_REPLY: {
					auto information = reinterpret_cast<const ip::icmp::information*>(
						reinterpret_cast<const char*>(icmp) + LENGTH);

					packer.pack_map(2);

					packer.pack("identifier");
					packer.pack_uint16(ntohs(information->identifier));

					packer.pack("sequence");
					packer.pack_uint16(ntohs(information->sequence));

					LENGTH += sizeof(ip::icmp::information);

					break;
				}

				case ip::icmp::PARAMETER_PROBLEM: {
					auto parameter_problem = reinterpret_cast<const ip::icmp::parameter_problem*>(
						reinterpret_cast<const char*>(icmp) + LENGTH);

					packer.pack_map(2);

					packer.pack("pointer");
					packer.pack_uint8(parameter_problem->pointer);

					packer.pack("header");
					auto offset = ip::pack(packer, reinterpret_cast<const struct ip*>(
						parameter_problem->payload));

					packer.pack("data");
					packer.pack_bin(8);
					packer.pack_bin_body(parameter_problem->payload + offset, 8);

					LENGTH += sizeof(ip::icmp::parameter_problem) + offset + 8;

					break;
				}

				case ip::icmp::REDIRECT_MESSAGE: {
					auto redirect_message = reinterpret_cast<const ip::icmp::redirect_message*>(
						reinterpret_cast<const char*>(icmp) + LENGTH);

					packer.pack_map(2);

					packer.pack("gateway");
					packer.pack(ip::address(&redirect_message->gateway));

					packer.pack("header");
					auto offset = ip::pack(packer, reinterpret_cast<const struct ip*>(
						redirect_message->payload));

					packer.pack("data");
					packer.pack_bin(8);
					packer.pack_bin_body(redirect_message->payload + offset, 8);

					LENGTH += sizeof(ip::icmp::redirect_message) + offset + 8;

					break;
				}

				case ip::icmp::SOURCE_QUENCH:
				case ip::icmp::DESTINATION_UNREACHABLE:
				case ip::icmp::TIME_EXCEEDED: {
					auto previous = reinterpret_cast<const ip::icmp::previous*>(
						reinterpret_cast<const char*>(icmp) + LENGTH);

					packer.pack_map(2);

					packer.pack("header");
					auto offset = ip::pack(packer, reinterpret_cast<const struct ip*>(
						previous->payload));

					packer.pack("data");
					packer.pack_bin(8);
					packer.pack_bin_body(previous->payload + offset, 8);

					LENGTH += sizeof(ip::icmp::previous) + offset + 8;

					break;
				}
			}

			if (header->len > OFFSET + LENGTH) {
				unknown(packer, header, OFFSET + LENGTH, reinterpret_cast<const char*>(icmp) + LENGTH);
			}
		}
	}
}
