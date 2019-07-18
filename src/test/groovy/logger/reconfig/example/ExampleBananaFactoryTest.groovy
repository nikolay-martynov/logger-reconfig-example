package logger.reconfig.example

import groovy.util.logging.Log4j2
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicLong

@Log4j2
class ExampleBananaFactoryTest extends Specification {

    def "will serve even under stress"() {
        given: "few good bananas and lots of bad ones in storage"
        Map<String, Tuple2<Boolean, Long>> storage =
                ["Ecuador": [true, 10000] as Tuple2,
                 "Norway" : [false, 1000000] as Tuple2]
        ExampleBananaFactory factory = new ExampleBananaFactory(
                storage, "Norway")
        and: "high demand for good bananas including missing country of origin"
        Map<String, Long> demand = ["Ecuador": 1000000,
                                    "Brazil" : 10000]
        and: "lots of requests going in parallel"
        AtomicLong good = new AtomicLong()
        AtomicLong bad = new AtomicLong()
        List<Thread> clients = demand.collectMany { origin, count ->
            (0..<10).collect {
                new Thread({
                    (0..<count / 10).each {
                        Banana result = factory.getBananaFrom(origin)
                        (result.tasty ? good : bad).incrementAndGet()
                    }
                })
            }
        }
        when: "all clients are served"
        clients*.start()
        clients*.join()
        then: "number of served good and bad bananas match what we had in store"
        good.get() == storage["Ecuador"].second
        bad.get() == storage["Norway"].second
    }
}
