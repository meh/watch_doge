#include <wd/sniffer>
#include <wd/packet/ether>

namespace wd {
	static
	std::tuple<pcap_t*, bpf_u_int32>
	_start(std::string device)
	{
		char errbuf[PCAP_ERRBUF_SIZE];

		bpf_u_int32 mask;
		bpf_u_int32 net;
		assert(pcap_lookupnet(device.c_str(), &net, &mask, errbuf) >= 0);

		pcap_t* session = pcap_open_live(device.c_str(), BUFSIZ, 1, 1000, errbuf);
		assert(session != NULL);

		return std::make_tuple(session, net);
	}

	static
	std::optional<std::string>
	_filter(pcap_t* session, std::optional<std::string> filter, bpf_u_int32 net)
	{
		struct bpf_program filt;

		if (filter) {
			if (pcap_compile(session, &filt, filter->c_str(), 1, net) < 0) {
				return std::string(pcap_geterr(session));
			}
		}
		else {
			pcap_compile(session, &filt, "", 1, net);
		}

		if (pcap_setfilter(session, &filt) < 0) {
			return std::string(pcap_geterr(session));
		}

		return std::nullopt;
	}

	static
	void
	_stop(pcap_t* session)
	{
		pcap_close(session);
	}

	static
	void
	_loop(int id, std::string device, std::shared_ptr<sniffer::cache> cache, std::shared_ptr<queue<sniffer::command>> queue) {
		pcap_t*     session = NULL;
		bpf_u_int32 netmask = 0;

		sniffer::command   command;

		while (true) {
			if (session == NULL) {
				queue->wait_dequeue(command);
			}
			else {
				if (!queue->try_dequeue(command)) {
					command.type = -1;
				}
			}

			if (command.type == command::sniffer::START && session != NULL) {
				wd::response(command::CONTROL, command.request, [](auto& packer) {
					packer.pack_uint8(command::sniffer::error::ALREADY_STARTED);
				});

				continue;
			}
			
			if (command.type != command::sniffer::START && session == NULL) {
				wd::response(command::CONTROL, command.request, [](auto& packer) {
					packer.pack_uint8(command::sniffer::error::NOT_STARTED);
				});

				continue;
			}

			switch (command.type) {
				case command::sniffer::START: {
					auto started = _start(device);

					session = std::get<0>(started);
					netmask = std::get<1>(started);

					wd::response(command::CONTROL, command.request, [](auto& packer) {
						packer.pack_uint8(command::SUCCESS);
					});

					break;
				}

				case command::sniffer::STOP: {
					_stop(session);

					wd::response(command::CONTROL, command.request, [](auto& packer) {
						packer.pack_uint8(command::SUCCESS);
					});

					break;
				}

				case command::sniffer::FILTER: {
					auto error = _filter(session, command.data.get<std::optional<std::string>>(), netmask);

					if (error) {
						wd::response(command::CONTROL, command.request, [&](auto& packer) {
							packer.pack_uint8(command::sniffer::error::INVALID_FILTER);
							packer.pack(*error);
						});
					}
					else {
						wd::response(command::CONTROL, command.request, [&](auto& packer) {
							packer.pack_uint8(command::SUCCESS);
						});
					}

					break;
				}

				default: {
					struct pcap_pkthdr header;
					const  u_char*     packet = pcap_next(session, &header);
					auto               pid    = cache->add(&header, packet);

					wd::response(command::SNIFFER, id, [&](auto& packer) {
						wd::packet::pack(packer, pid, &header, packet);
					});

					break;
				}
			}
		}

		_stop(session);
	}

	sniffer::sniffer(int id, std::string device, std::string cache, uint32_t truncate)
		: _id(id),
		  _device(device),
		  _queue(std::make_shared<queue<sniffer::command>>(1))
	{
		if (truncate == 0) {
			_cache = std::make_shared<sniffer::cache>(cache, id);
		}
		else {
			_cache = std::make_shared<sniffer::cache>(cache, id, truncate);
		}

		_thread  = std::thread(_loop, _id, _device, _cache, _queue);
		_thread.detach();
	}

	sniffer::~sniffer()
	{
		_queue->enqueue(sniffer::command {
			.request = 0,
			.type    = wd::command::sniffer::DESTROY,
		});
	}

	void
	sniffer::start(int request)
	{
		_queue->enqueue(sniffer::command {
			.request = request,
			.type    = wd::command::sniffer::START,
		});
	}

	void
	sniffer::filter(int request, std::optional<std::string> flt)
	{
		_queue->enqueue(sniffer::command {
			.request = request,
			.type    = wd::command::sniffer::FILTER,
			.data    = flt,
		});
	}

	void
	sniffer::stop(int request)
	{
		_queue->enqueue(sniffer::command {
			.request = request,
			.type    = wd::command::sniffer::STOP,
		});
	}

	std::optional<std::tuple<const packet::header*, const uint8_t*>>
	sniffer::get(uint32_t id)
	{
		return _cache->get(id);
	}

	sniffer::cache::cache(std::string path, int id, uint32_t truncate)
		: _id(0), _offset(sizeof(sniffer::cache::header)), _truncate(truncate)
	{
		std::stringstream builder;

		builder << path << "/" << id << ".pcap";
		_session = builder.str();

		builder << ".index";
		_index = builder.str();

		_isession.open(_session, std::ios::binary);
		_osession.open(_session, std::ios::binary | std::ios::trunc);

		_iindex.open(_index, std::ios::binary);
		_oindex.open(_index, std::ios::binary | std::ios::trunc);

		sniffer::cache::header header = {
			.magic_number = 0xA1B2C3D4,

			.version_major = 2,
			.version_minor = 4,

			.thiszone = 0,
			.sigfigs  = 0,

			.snaplen = truncate,
			.network = 1,
		};

		_osession.write(reinterpret_cast<const char*>(&header), sizeof(sniffer::cache::header));
		_osession.flush();
	}

	uint32_t
	sniffer::cache::add(const packet::header* header, const uint8_t* packet)
	{
		uint32_t       id    = _id++;
		packet::header entry = *header;

		if (entry.caplen > _truncate) {
			entry.caplen = _truncate;
		}

		_osession.write(reinterpret_cast<const char*>(&entry), sizeof(packet::header));
		_osession.write(reinterpret_cast<const char*>(packet), entry.caplen);
		_osession.flush();

		_offset += sizeof(packet::header);
		_offset += entry.caplen;

		_oindex.write(reinterpret_cast<const char*>(&_offset), 4);
		_oindex.flush();

		return id;
	}

	std::optional<std::tuple<const packet::header*, const uint8_t*>>
	sniffer::cache::get(uint32_t id)
	{
		if (id > _id) {
			return std::nullopt;
		}

		uint32_t offset;

		_iindex.seekg(id * 4, std::fstream::beg);
		_iindex.read(reinterpret_cast<char*>(&offset), 4);

		_isession.seekg(offset, std::fstream::beg);
		_isession.read(reinterpret_cast<char*>(&_header), sizeof(packet::header));

		_buffer.resize(_header.caplen);
		_isession.read(reinterpret_cast<char*>(_buffer.data()), _header.caplen);

		return std::make_tuple(
			reinterpret_cast<const packet::header*>(&_header),
			reinterpret_cast<const uint8_t*>(_buffer.data()));
	}
}
