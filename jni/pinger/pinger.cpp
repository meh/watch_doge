#include "pinger"

namespace wd {
	static
	void
	_loop(int id, std::string target, int64_t interval, std::shared_ptr<queue<pinger::command>> queue)
	{
		pcap_t*          session = NULL;
		sniffer::command command;

		while (true) {
			if (session == NULL) {
				queue->wait_dequeue(command);
			}
			else {
				if (!queue->try_dequeue(command)) {
					command.type = -1;
				}
			}

			switch (command.type) {

			}
		}
	}

	pinger::pinger(int id, std::string target, int64_t interval)
		: _id(id),
		  _target(target),
		  _interval(interval)
	{
		_thread = std::thread(_loop, _id, _target, _interval, _queue);
		_thread.detach();
	}

	pinger::~pinger()
	{
		_queue->enqueue(sniffer::command {
			.request = 0,
			.type    = wd::command::pinger::DESTROY,
		});
	}
}
