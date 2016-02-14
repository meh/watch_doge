#include "pinger"

#include <netdb.h>
#include <fcntl.h>

namespace wd {
	static
	void
	_loop(wd::creator<>* creation, int id, int request, std::string target, uint32_t interval, std::shared_ptr<queue<pinger::command>> queue)
	{
		struct hostent* host = gethostbyname(target.c_str());

		if (host == NULL) {
			creation->err(id, request, command::pinger::error::INVALID_TARGET);
			return;
		}

		int sock = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
		if (sock < 0) {
			creation->err(id, request, command::pinger::error::SOCKET_ERROR);
			return;
		}

		{
    	int flags = fcntl(sock, F_GETFL, 0);
			if (flags < 0) {
				creation->err(id, request, command::pinger::error::SOCKET_ERROR);
				return;
			}

    	if (fcntl(sock, F_SETFL, flags | O_NONBLOCK) < 0) {
				creation->err(id, request, command::pinger::error::SOCKET_ERROR);
				return;
			}
		}

		creation->ok(id, request);

		LOG(DEBUG, "pinger/create: id=%d target=%s interval=%d", id, target.c_str(), interval);

		bool            paused = true;
		pinger::command command;

		while (true) {
			if (paused) {
				queue->wait_dequeue(command);
			}
			else {
				if (!queue->try_dequeue(command)) {
					command.type = -1;
				}
			}

			switch (command.type) {
				case command::pinger::START: {
					if (!paused) {
						wd::response(command::CONTROL, command.request, [](auto& packer) {
							packer.pack_uint8(command::pinger::error::ALREADY_STARTED);
						});

						continue;
					}

					paused = false;

					wd::response(command::CONTROL, command.request, [](auto& packer) {
						packer.pack_uint8(command::SUCCESS);
					});

					break;
				}

				case command::pinger::STOP: {
					if (paused) {
						wd::response(command::CONTROL, command.request, [](auto& packer) {
							packer.pack_uint8(command::pinger::error::NOT_STARTED);
						});

						continue;
					}

					paused = true;

					wd::response(command::CONTROL, command.request, [](auto& packer) {
						packer.pack_uint8(command::SUCCESS);
					});

					break;
				}

				case command::pinger::DESTROY: {
					close(sock);

					wd::response(command::CONTROL, command.request, [](auto& packer) {
						packer.pack_uint8(command::SUCCESS);
					});

					return;
				}

				default: {
					LOG(DEBUG, "pinger/idle: id=%d", id);

					std::this_thread::sleep_for(std::chrono::milliseconds(interval));
				}
			}
		}
	}

	pinger::pinger(wd::creator<>* creator, int id, int request, std::string target, uint32_t interval)
		: _id(id),
		  _target(target),
		  _queue(std::make_shared<queue<pinger::command>>(1))
	{
		_interval = interval ?: 1000;

		_thread = std::thread(_loop, creator, _id, request, _target, _interval, _queue);
		_thread.detach();
	}

	pinger::~pinger()
	{
		_queue->enqueue(pinger::command {
			.request = 0,
			.type    = wd::command::pinger::DESTROY,
		});
	}

	void
	pinger::start(int request)
	{
		_queue->enqueue(command {
			.request = request,
			.type    = wd::command::pinger::START
		});
	}

	void
	pinger::stop(int request)
	{
		_queue->enqueue(command {
			.request = request,
			.type    = wd::command::pinger::STOP
		});
	}

	void
	pinger::destroy(int request)
	{
		_queue->enqueue(command {
			.request = request,
			.type    = wd::command::pinger::DESTROY
		});
	}
}
