#include <wd/packet/unknown>
#include <wd/packet/ether>
#include <wd/packet/ip>
#include <wd/packet/tcp>

namespace wd {
	namespace packet {
		uint16_t
		tcp::source() const
		{
			return ntohs(packet->source);
		}

		uint16_t
		tcp::destination() const
		{
			return ntohs(packet->destination);
		}

		uint32_t
		tcp::sequence() const
		{
			return ntohl(packet->sequence);
		}

		uint32_t
		tcp::acknowledgement() const
		{
			return ntohl(packet->acknowledgement);
		}

		uint8_t
		tcp::offset() const
		{
			return ntohs(packet->offset_and_flags) >> 12;
		}

		std::bitset<6>
		tcp::flags() const
		{
			return ntohs(packet->offset_and_flags) & 0x3f;
		}

		bool
		tcp::urg() const
		{
			return flags().test(5);
		}

		bool
		tcp::ack() const
		{
			return flags().test(4);
		}

		bool
		tcp::psh() const
		{
			return flags().test(3);
		}

		bool
		tcp::rst() const
		{
			return flags().test(2);
		}

		bool
		tcp::syn() const
		{
			return flags().test(1);
		}

		bool
		tcp::fin() const
		{
			return flags().test(0);
		}

		uint16_t
		tcp::window() const
		{
			return ntohs(packet->window);
		}

		uint16_t
		tcp::checksum() const
		{
			return ntohs(packet->checksum);
		}

		uint16_t
		tcp::urgent() const
		{
			return ntohs(packet->urgent);
		}

		const char*
		tcp::data() const
		{
			return reinterpret_cast<const char*>(packet) + (offset() * 4);
		}

		size_t
		tcp::pack(msgpack::packer<std::ostream>& packer, const packet::header* header, const ether::raw* ether, const ip::raw* ip)
		{
			auto OFFSET = reinterpret_cast<const char*>(packet) - reinterpret_cast<const char*>(ether);
			auto LENGTH = offset() * 4;

			packer.pack("tcp");
			packer.pack_map(8);

			packer.pack("source");
			packer.pack_uint16(source());

			packer.pack("destination");
			packer.pack_uint16(destination());

			packer.pack("sequence");
			packer.pack_uint32(sequence());

			packer.pack("acknowledgement");
			packer.pack_uint32(acknowledgement());

			packer.pack("flags");
			packer.pack_array(flags().count());
			if (urg()) packer.pack("urg");
			if (ack()) packer.pack("ack");
			if (psh()) packer.pack("psh");
			if (rst()) packer.pack("rst");
			if (syn()) packer.pack("syn");
			if (fin()) packer.pack("fin");

			packer.pack("window");
			packer.pack_uint16(window());

			packer.pack("checksum");
			packer.pack_uint16(checksum());

			packer.pack("urgent");
			packer.pack_uint16(urgent());

			// TODO: recognize protocol
			unknown unknown(header, OFFSET + LENGTH, data());
			unknown.pack(packer);
		}
	}
}
