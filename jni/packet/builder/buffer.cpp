#include <packet/builder/buffer>

namespace wd {
	namespace packet {
		namespace builder {
			void
			buffer::next(size_t size)
			{
				_offset   = _total;
				_partial  = size;
				_total   += size;
				_data     = reinterpret_cast<uint8_t*>(std::realloc(_data, _total));

				std::memset(_data + _offset, 0, size);
			}

			void
			buffer::more(size_t size)
			{
				_total += size;
				_data   = reinterpret_cast<uint8_t*>(std::realloc(_data, _total));

				std::memset(_data + _offset + _partial, 0, size);

				_partial += size;
			}

			uint8_t*
			buffer::data()
			{
				return _data + _offset;
			}

			size_t
			buffer::length() const
			{
				return _partial;
			}

			uint8_t*
			buffer::whole()
			{
				return _data;
			}

			size_t
			buffer::total() const
			{
				return _total;
			}
		}
	}
}
