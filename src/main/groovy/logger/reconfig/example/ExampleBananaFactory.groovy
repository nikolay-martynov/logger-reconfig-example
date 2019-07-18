package logger.reconfig.example

import groovy.util.logging.Log4j2

import java.util.concurrent.atomic.AtomicLong

/**
 * Example banana factory that will draw from reserve when cannot
 * serve from requested origin.
 *
 * This class is thread safe and bananas can be requested from multiple threads.
 */
@Log4j2
class ExampleBananaFactory implements BananaFactory {

    private final Map<String, Tuple2<Boolean, AtomicLong>> storage

    private final String reserveOrigin

    /**
     * Creates new instance with specified storage.
     * @param storage Storage to serve from.
     * Key is origin, first value indicates if those bananas
     * are tasty and second value
     * indicates how much we have them.
     * @param reserveOrigin Name of reserve origin to draw from when
     * cannot fulfill request from specified origin.
     */
    ExampleBananaFactory(Map<String, Tuple2<Boolean, Long>> storage,
                         String reserveOrigin) {
        this.storage = storage.collectEntries { origin, info ->
            [origin, [info.first, new AtomicLong(info.second)] as Tuple2]
        }
        this.reserveOrigin = reserveOrigin
    }

    @Override
    Banana getBananaFrom(String origin) {
        Tuple2<Boolean, AtomicLong> requestedInfo = storage[origin]
        while (true) {
            long availableFromRequested = requestedInfo?.second?.get() ?: 0
            if (availableFromRequested > 0) {
                if (requestedInfo.second.compareAndSet(
                        availableFromRequested,
                        availableFromRequested - 1)) {
                    return new ExampleBanana(requestedInfo.first)
                }
            } else {
                log.warn("Cannot serve from $origin!" +
                        " Will draw from reserves in $reserveOrigin!!!")
                Tuple2<Boolean, AtomicLong> reserveInfo = storage[reserveOrigin]
                while (true) {
                    long availableFromReserve = reserveInfo?.second?.get() ?: 0
                    if (availableFromReserve > 0) {
                        if (reserveInfo.second.compareAndSet(
                                availableFromReserve,
                                availableFromReserve - 1)) {
                            return new ExampleBanana(reserveInfo.first)
                        }
                    } else {
                        throw new IllegalStateException(
                                'Nah! This could never happen!')
                    }
                }
            }
        }
    }

}
