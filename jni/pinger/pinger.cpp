#include "pinger"

#include <paku/builder/icmp>
#include <paku/packet/limits>
#include <paku/layer>

namespace wd {
	const uint32_t GRANULARITY = 100;

	struct stats {
		uint32_t sent;
		uint32_t recv;

		uint32_t min;
		uint32_t max;
		double   avg;
	};

	static
	void
	_stats(msgpack::packer<std::ostream>& packer, struct stats* stats)
	{
		packer.pack(command::event::pinger::STATS);

		packer.pack_int(stats->sent);
		packer.pack_int(stats->recv);

		if (stats->sent != stats->recv) {
			packer.pack_float(((stats->sent - (stats->recv + 1)) * 100.0) / stats->sent);
		}
		else {
			packer.pack_float(0);
		}

		packer.pack_int(stats->min);
		packer.pack_int(stats->max);
		packer.pack_int((stats->avg / stats->recv) * 1'000'000);
	}

	static
	void
	_analyze(int id, struct stats* stats, paku::layer<paku::packet::ip> ip)
	{
		auto icmp = ip.icmp();

		icmp.parent()->icmp();

		switch (icmp->type()) {
			case paku::packet::icmp::ECHO_REPLY: {
				auto echo = icmp->details<paku::packet::icmp::echo>();

				if (echo.identifier() != id) {
					return;
				}

				struct timeval now;
				gettimeofday(&now, nullptr);

				const struct timeval* then
					= reinterpret_cast<const struct timeval*>(echo.data());

				uint32_t sec  = (now.tv_sec - then->tv_sec) * 1'000'000;
				int32_t  usec = (now.tv_usec - then->tv_usec);

				if (usec < 0) {
					sec  -= 1'000'000;
					usec += 1'000'000;
				}

				uint32_t trip = sec + usec;

				stats->min   = std::min(stats->min, trip);
				stats->max   = std::max(stats->max, trip);
				stats->avg  += trip / 1'000'000.0l;
				stats->recv += 1;

				wd::response(command::PINGER, id, [&](auto& packer) {
					_stats(packer, stats);
				});

				wd::response(command::PINGER, id, [&](auto& packer) {
					packer.pack(command::event::pinger::PACKET);

					packer.pack(ip->source());
					packer.pack(echo.sequence());
					packer.pack(ip->ttl());
					packer.pack(trip);
				});

				break;
			}

			case paku::packet::icmp::DESTINATION_UNREACHABLE:
			case paku::packet::icmp::SOURCE_QUENCH:
			case paku::packet::icmp::TIME_EXCEEDED: {
				auto previous = icmp->details<paku::packet::icmp::previous>();

				if (previous.header().protocol() != paku::packet::ip::ICMP) {
					return;
				}

				paku::packet::icmp prev(previous.data());

				if (prev.type() != paku::packet::icmp::ECHO_REQUEST) {
					return;
				}

				auto echo = prev.details<paku::packet::icmp::echo>();

				if (echo.identifier() != id) {
					return;
				}

				wd::response(command::PINGER, id, [&](auto& packer) {
					packer.pack(command::event::pinger::ERROR);

					packer.pack(ip->source());
					packer.pack(echo.sequence());
					packer.pack(ip->ttl());

					switch (icmp->type()) {
						case paku::packet::icmp::DESTINATION_UNREACHABLE: {
							if (auto string = paku::to_string(icmp->code<paku::packet::icmp::code::destination_unreachable>())) {
								packer.pack(*string);
							}
							else {
								packer.pack(*paku::to_string(icmp->type()));
							}

							break;
						}

						default:
							packer.pack("unknown");
					}
				});

				break;
			}
		}
	}

	static
	void
	_loop(wd::creator<>* creation, int id, int request, pinger::settings settings, std::shared_ptr<queue<pinger::command>> queue)
	{
		struct hostent* host = ::gethostbyname(settings.target.c_str());

		if (host == NULL) {
			creation->err(id, request, command::pinger::error::UNKNOWN_HOST);
			return;
		}

		int sock = ::socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
		if (sock < 0) {
			creation->err(id, request, command::pinger::error::SOCKET);
			return;
		}

		if (socket::timeout(sock, std::chrono::milliseconds(GRANULARITY)) < 0) {
			creation->err(id, request, command::pinger::error::SOCKET);
			return;
		}

		struct sockaddr_in to;
		std::memcpy(&to.sin_addr, host->h_addr, sizeof(to.sin_addr));

		creation->ok(id, request);

		uint8_t buffer[
			paku::packet::limits<paku::packet::ip>::max() +
			paku::packet::limits<paku::packet::icmp>::max() +
			sizeof(struct timeval)] = { 0 };

		struct sockaddr_in from;
		socklen_t          from_s;

		bool            paused = true;
		uint32_t        slept  = 0;
		pinger::command command;

		struct stats stats = {
			.sent = 0,
			.recv = 0,

			.min = std::numeric_limits<uint32_t>::max(),
			.max = std::numeric_limits<uint32_t>::min(),
			.avg = 0,
		};

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
					ssize_t  length = 0;
					uint32_t sleep  = GRANULARITY;

					if (slept + GRANULARITY > settings.interval) {
						sleep = settings.interval - slept;
					}

					length = recvfrom(sock, buffer, sizeof(buffer),
						0, reinterpret_cast<struct sockaddr*>(&from), &from_s);

					if (length >= 0) {
						_analyze(id, &stats, paku::layer<paku::packet::ip>(buffer, length));

						wd::socket::nonblocking(sock);
						while (true) {
							length = recvfrom(sock, buffer, sizeof(buffer),
								0, reinterpret_cast<struct sockaddr*>(&from), &from_s);

							if (length < 0) {
								break;
							}

							_analyze(id, &stats, paku::layer<paku::packet::ip>(buffer, length));
						}
						wd::socket::blocking(sock);
					}

					slept += GRANULARITY;

					if (slept >= settings.interval) {
						slept = 0;

						struct timeval now;
						gettimeofday(&now, nullptr);

						paku::builder::icmp builder;
						auto buffer = builder.echo().request()
							.identifier(id)
							.sequence(stats.sent)
							.data(&now, sizeof(now))
							.build();

						length = sendto(sock, buffer->whole(), buffer->total(),
							0, reinterpret_cast<sockaddr*>(&to), sizeof(to));

						if (length == buffer->total()) {
							stats.sent += 1;

							wd::response(command::PINGER, id, [&](auto& packer) {
								_stats(packer, &stats);
							});
						}
					}
				}
			}
		}
	}

	pinger::pinger(wd::creator<>* creator, int id, int request, pinger::settings settings)
		: _id(id),
		  _queue(std::make_shared<queue<pinger::command>>(1))
	{
		_thread = std::thread(_loop, creator, _id, request, settings, _queue);
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
