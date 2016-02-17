#include <wd/common>
#include <wd/socket>

namespace wd {
	namespace socket {
		int
		blocking(int sock)
		{
			int result;

    	result = fcntl(sock, F_GETFL, 0);
			if (result < 0) {
				return result;
			}

			result = fcntl(sock, F_SETFL, result & ~O_NONBLOCK);
			if (result < 0) {
				return result;
			}

			return 0;
		}

		int
		nonblocking(int sock)
		{
			int result;

    	result = fcntl(sock, F_GETFL, 0);
			if (result < 0) {
				return result;
			}

			result = fcntl(sock, F_SETFL, result | O_NONBLOCK);
			if (result < 0) {
				return result;
			}

			return 0;
		}

		int
		timeout(int sock, std::chrono::milliseconds time)
		{
			struct timeval timeout = {
				.tv_sec = static_cast<time_t>(
					std::chrono::duration_cast<std::chrono::seconds>(time).count()),

				.tv_usec = static_cast<time_t>(
					std::chrono::duration_cast<std::chrono::microseconds>(time).count()),
			};

			return ::setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
		}
	}
}
