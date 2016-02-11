#include <wd/common>

namespace wd {
	size_t
	receiver::parse()
	{
		if (_unpacker.nonparsed_size() != 0) {
			return 0;
		}

		size_t consumed = _chunk;
		size_t total    = 0;

		while (consumed == _chunk) {
			_unpacker.reserve_buffer(_chunk);
			consumed = read(0, _unpacker.buffer(), _chunk);
			_unpacker.buffer_consumed(consumed);
			total += consumed;
		}

		return total;
	}

	const msgpack::object&
	receiver::next()
	{
		_unpacker.next(_unpacked);
		return _unpacked.get();
	}
}
