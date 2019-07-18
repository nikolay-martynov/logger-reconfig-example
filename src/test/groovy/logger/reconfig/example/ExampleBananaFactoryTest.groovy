package logger.reconfig.example

import groovy.util.logging.Log4j2
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.LoggerConfig
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicLong

@Log4j2
class ExampleBananaFactoryTest extends Specification {

    def "will serve from reserve if cannot do as requested"() {
        given: "only reserve is available"
        ExampleBananaFactory factory = new ExampleBananaFactory(
                ["reserve": [true, 1] as Tuple2], "reserve")
        when: "have no bananas from requested origin"
        Banana result = factory.getBananaFrom("wonderland")
        then: "still gets a banana"
        result.tasty
    }

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
        and: "warnings are suppressed to avoid log spam"
        LoggerContext loggerContext = LogManager.getContext(false) as LoggerContext
        loggerContext.configuration.addLogger(
                ExampleBananaFactory.name,
                LoggerConfig.createLogger(
                        true,
                        Level.ERROR,
                        ExampleBananaFactory.name,
                        "false",
                        loggerContext.configuration.appenders.collect {
                            AppenderRef.createAppenderRef(
                                    it.key,
                                    null,
                                    null)
                        } as AppenderRef[],
                        null,
                        loggerContext.configuration,
                        null))
        loggerContext.updateLoggers()
        when: "all clients are served"
        clients*.start()
        clients*.join()
        then: "number of served good and bad bananas match what we had in store"
        good.get() == storage["Ecuador"].second
        bad.get() == storage["Norway"].second
        cleanup:
        loggerContext.configuration.removeLogger(ExampleBananaFactory.name)
        loggerContext.updateLoggers()
    }
}
