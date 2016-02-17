#include <packet/builder/icmp>

namespace wd {
	namespace packet {
		namespace builder {
			icmp::icmp(std::shared_ptr<builder::buffer> buf) : base(buf)
			{
				_buffer->next(8);
			}

			icmp::icmp() : icmp(std::shared_ptr<builder::buffer>(new builder::buffer))
			{ }

			packet::icmp::raw*
			icmp::packet()
			{
				return reinterpret_cast<packet::icmp::raw*>(
					buffer()->data());
			}

			std::shared_ptr<builder::buffer>
			icmp::build()
			{
				packet()->checksum = packet::ip::checksum(_buffer->data(), _buffer->length());

				return _buffer;
			}

			class icmp::echo
			icmp::echo()
			{
				return *this;
			}

			icmp::echo::echo(icmp& parent) : _parent(parent)
			{ }

			packet::icmp::echo::raw*
			icmp::echo::packet()
			{
				return reinterpret_cast<packet::icmp::echo::raw*>(
					_parent.buffer()->data() + 4);
			}

			std::shared_ptr<builder::buffer>
			icmp::echo::build()
			{
				return _parent.build();
			}

			class icmp::echo&
			icmp::echo::request()
			{
				_parent.packet()->type = packet::icmp::ECHO_REQUEST;

				return *this;
			}

			class icmp::echo&
			icmp::echo::reply()
			{
				_parent.packet()->type = packet::icmp::ECHO_REPLY;

				return *this;
			}

			class icmp::echo&
			icmp::echo::identifier(uint16_t value)
			{
				packet()->identifier = htons(value);

				return *this;
			}

			class icmp::echo&
			icmp::echo::sequence(uint16_t value)
			{
				packet()->sequence = htons(value);

				return *this;
			}

			class icmp::echo&
			icmp::echo::data(uint8_t* data, size_t length)
			{
				_parent.buffer()->more(length);
				std::memcpy(_parent.buffer()->data() + 8, data, length);

				return *this;
			}
		}
	}
}
