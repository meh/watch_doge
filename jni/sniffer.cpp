#include <wd/sniffer>
#include <wd/packet/ether>

namespace wd {
	static
	std::tuple<pcap_t*, bpf_u_int32>
	_start(string device)
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
	optional<std::string>
	_filter(pcap_t* session, optional<string> filter, bpf_u_int32 net)
	{
		struct bpf_program filt;

		if (filter) {
			if (pcap_compile(session, &filt, filter->c_str(), 1, net) < 0) {
				return optional<std::string>(pcap_geterr(session));
			}
		}
		else {
			pcap_compile(session, &filt, "", 1, net);
		}

		if (pcap_setfilter(session, &filt) < 0) {
			return optional<std::string>(pcap_geterr(session));
		}

		return optional<std::string>();
	}

	static
	void
	_stop(pcap_t* session)
	{
		pcap_close(session);
	}

	static
	void
	_loop(int id, string device, shared_ptr<queue<sniffer::command>> queue) {
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
					auto error = _filter(session, command.data.get<optional<std::string>>(), netmask);

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

					wd::response(command::SNIFFER, id, [&](auto& packer) {
						// packet size
						packer.pack_uint32(header.len);

						// packet timestamp
						packer.pack_int64(header.ts.tv_sec);
						packer.pack_int64(header.ts.tv_usec);

						// start from ethernet, it does the rest itself
						packet::ether ether(reinterpret_cast<const packet::ether::raw*>(packet));
						ether.pack(packer, &header);

						// no more decoded data
						packer.pack_nil();
					});

					break;
				}
			}
		}

		_stop(session);
	}

	sniffer::sniffer(int id, string device)
		: _id(id), _device(device), _queue(std::make_shared<queue<sniffer::command>>(1))
	{
		_thread  = thread(_loop, _id, _device, _queue);
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
	sniffer::filter(int request, optional<string> flt)
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
}
