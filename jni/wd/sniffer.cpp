#include <pcap.h>

#include <wd/send>
#include <wd/sniffer>

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
	pcap_t*
	_filter(pcap_t* session, optional<string> filter, bpf_u_int32 net)
	{
		struct bpf_program filt;

		if (filter) {
			assert(pcap_compile(session, &filt, filter->c_str(), 0, net) >= 0);
		}
		else {
			assert(pcap_compile(session, &filt, "", 0, net) >= 0);
		}

		assert(pcap_setfilter(session, &filt) >= 0);

		return session;
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

		struct pcap_pkthdr header;
		const  u_char*     packet;
		sniffer::command   command;

		while (true) {
			if (session == NULL) {
				queue->wait_dequeue(command);
			}
			else {
				if (!queue->try_dequeue(command)) {
					command.type = sniffer::command::idle;
				}
			}

			switch (command.type) {
				case sniffer::command::Type::start:
					if (session == NULL) {
						auto started = _start(device);

						session = std::get<0>(started);
						netmask = std::get<1>(started);
					}

					break;

				case sniffer::command::Type::stop:
					if (session != NULL) {
						_stop(session);
					}

					break;

				case sniffer::command::Type::filter:
					if (session != NULL) {
						_filter(session, *command.data.filter, netmask);
						delete command.data.filter;
					}

					break;

				case sniffer::command::Type::idle:
					if (session != NULL) {
						packet = pcap_next(session, &header);

						wd::send(id, [](msgpack::packer<std::ostream>& packer) {
							packer.pack_true();
						});
					}

					break;
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
		stop();
	}

	void
	sniffer::start()
	{
		_queue->enqueue(sniffer::command {
			.type = sniffer::command::Type::start
		});
	}

	void
	sniffer::filter(optional<string> flt)
	{
		_queue->enqueue(sniffer::command {
			.type = sniffer::command::Type::filter,
			.data = {
				.filter = new optional<string>(flt)
			}
		});
	}

	void
	sniffer::stop()
	{
		_queue->enqueue(sniffer::command {
			.type = sniffer::command::Type::stop
		});
	}
}
