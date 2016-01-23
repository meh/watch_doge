#include <wd/device>

namespace wd {
	namespace device {
		optional<std::string>
		find(std::string ip)
		{
			char                  errbuf[PCAP_ERRBUF_SIZE];
			pcap_if_t*            devices;
			optional<std::string> result;

			if (pcap_findalldevs(&devices, errbuf) < 0) {
				return result;
			}

			struct in_addr ip_addr;
			inet_pton(AF_INET, ip.c_str(), &ip_addr);

			pcap_if_t* device = devices;
			while (device != NULL) {
				pcap_addr_t* address = device->addresses;
				while (address != NULL) {
					struct sockaddr_in* addr = (struct sockaddr_in*) address->addr;

					if (addr->sin_family == AF_INET && addr->sin_addr.s_addr == ip_addr.s_addr) {
						result.emplace(device->name);
						goto done;
					}

					address = address->next;
				}

				device = device->next;
			}

		done:
			pcap_freealldevs(devices);

			return result;
		}
	}
}
