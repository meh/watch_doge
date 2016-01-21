#include <wd/packet>
#include <wd/log>

namespace wd {
	namespace packet {
		void
		pack(msgpack::packer<std::ostream>& packer, const header* header, const ether* ether, const ip* ip, const ip::icmp* icmp)
		{
			auto OFFSET = sizeof(ip::icmp);

			packer.pack("icmp");
			packer.pack_map(3);

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

				case ip::icmp::TIMESTAMP:
					packer.pack("timestamp");
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
						case ip::icmp::DESTINATION_NETWORK_UNREACHABLE:
							packer.pack("destination-network-unreachable");
							break;

						case ip::icmp::DESTINATION_HOST_UNREACHABLE:
							packer.pack("destination-host-unreachable");
							break;

						case ip::icmp::DESTINATION_PROTOCOL_UNREACHABLE:
							packer.pack("destination-protocol-unreachable");
							break;

						case ip::icmp::DESTINATION_PORT_UNREACHABLE:
							packer.pack("destination-port-unreachable");
							break;

						case ip::icmp::FRAGMENTATION_REQUIRED:
							packer.pack("fragmentation-required");
							break;

						case ip::icmp::SOURCE_ROUTE_FAILED:
							packer.pack("source-route-failed");
							break;

						case ip::icmp::DESTINATION_NETWORK_UNKNOWN:
							packer.pack("destination-network-unknown");
							break;

						case ip::icmp::DESTINATION_HOST_UNKNOWN:
							packer.pack("destination-host-unknown");
							break;

						case ip::icmp::SOURCE_HOST_ISOLATED:
							packer.pack("source-host-isolated");
							break;

						case ip::icmp::NETWORK_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("network-administratively-prohibited");
							break;

						case ip::icmp::HOST_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("host-administratively-prohibited");
							break;

						case ip::icmp::NETWORK_UNREACHABLE_FOR_TOS:
							packer.pack("network-unreachable-for-tos");
							break;

						case ip::icmp::HOST_UNREACHABLE_FOR_TOS:
							packer.pack("host-unreachable-for-tos");
							break;

						case ip::icmp::COMMUNICATION_ADMINISTRATIVELY_PROHIBITED:
							packer.pack("communication-administratively-prohibited");
							break;

						case ip::icmp::HOST_PRECEDENCE_VIOLATION:
							packer.pack("host-precedence-violation");
							break;

						case ip::icmp::PRECEDENT_CUTOFF_IN_EFFECT:
							packer.pack("precedent-cutoff-in-effect");
							break;

						default:
							packer.pack_uint8(icmp->code.raw);
					}

					break;
				}

				case ip::icmp::REDIRECT_MESSAGE: {
					switch (icmp->code.redirect_message) {
						case ip::icmp::REDIRECT_DATAGRAM_FOR_NETWORK:
							packer.pack("redirect-datagram-for-network");
							break;

						case ip::icmp::REDIRECT_DATAGRAM_FOR_HOST:
							packer.pack("redirect-datagram-for-host");
							break;

						case ip::icmp::REDIRECT_DATAGRAM_FOR_TOS_AND_NETWORK:
							packer.pack("redirect-datagram-for-tos-and-network");
							break;

						case ip::icmp::REDIRECT_DATAGRAM_FOR_TOS_AND_HOST:
							packer.pack("redirect-datagram-for-tos-and-host");
							break;

						default:
							packer.pack_uint8(icmp->code.raw);
					}

					break;
				}

				case ip::icmp::PARAMETER_PROBLEM: {
					switch (icmp->code.parameter_problem) {
						case ip::icmp::POINTER_INDICATES_ERROR:
							packer.pack("pointer-indicates-error");
							break;

						case ip::icmp::MISSING_REQUIRED_OPTION:
							packer.pack("missing-required-option");
							break;

						case ip::icmp::BAD_LENGTH:
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
		}
	}
}
