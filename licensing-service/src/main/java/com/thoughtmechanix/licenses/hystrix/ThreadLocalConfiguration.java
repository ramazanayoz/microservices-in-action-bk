package com.thoughtmechanix.licenses.hystrix;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class ThreadLocalConfiguration {

    @Autowired(required = false)
    /*
    * When the configuration object
    * is constructud it will autowire
    * in the existing HystrixConcurrencyStrategy
    * */
    private HystrixConcurrencyStrategy existingConcurrencyStrategy;

    @PostConstruct
    public void init(){
        //Keeps references of existing Hystrix plugins.
        /*
        * Because you're registring a new concurrency strategy, you're
        * going to grab all the other Hystrix components and then reset
        * Hystrix plugin.
        * */
        HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
        HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
        HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();

        HystrixPlugins.reset();
        /*
        * You now register your HystrixConcurrencyStrategy (ThreadLocalAwareStrategy) with
        * the Hystrix plugin.
        * */
        HystrixPlugins.getInstance().registerConcurrencyStrategy(new ThreadLocalAwareStrategy(existingConcurrencyStrategy));
        /*
        * Then register all the Hystrix components used
        * by the Hystrix plugin
        * */
        HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
    }
}
