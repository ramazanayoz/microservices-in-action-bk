package com.thoughtmechanix.licenses.services;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.thoughtmechanix.licenses.clients.OrganizationDiscoveryClient;
import com.thoughtmechanix.licenses.clients.OrganizationFeignClient;
import com.thoughtmechanix.licenses.clients.OrganizationRestTemplateClient;
import com.thoughtmechanix.licenses.config.ServiceConfig;
import com.thoughtmechanix.licenses.model.License;
import com.thoughtmechanix.licenses.model.Organization;
import com.thoughtmechanix.licenses.repository.LicenseRepository;
import com.thoughtmechanix.licenses.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class LicenseService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

    @Autowired
    ServiceConfig config;
    @Autowired
    OrganizationFeignClient organizationFeignClient;
    @Autowired
    OrganizationRestTemplateClient organizationRestClient;
    @Autowired
    OrganizationDiscoveryClient organizationDiscoveryClient;
    @Autowired
    private LicenseRepository licenseRepository;

    /*
    * @HystrixCommand annotation is used to wrapper the
    * getLicanceByOrg() method with a Hystrix circuit braker.
    * */
    @HystrixCommand( /*The commandProperties attribute lets you provide additional properties customize Hystrix */
            fallbackMethod = "buildFallbackLicenseList", /*The fallbackMethod attribute defines a single function in your class that will be called if the call from Hystrix fails.*/
            threadPoolKey = "licenseByOrgThreadPool", /*The threadPoolKey attribute defines the unique name of theread pool*/
            threadPoolProperties = { /*The threadPoolProperties attribute lets you define and customize behaviour the threadPool*/
                    @HystrixProperty(name = "coreSize", value = "30"), /*The coreSize attribute lets you define the maximum number of threads in the thread pool.*/
                    @HystrixProperty(name = "maxQueueSize", value = "10"),/*The maxQueueSize lets you define a queue that sits in front of your thread pool abd that can queue incoming requests*/
            },
            commandProperties = {
                    @HystrixProperty(
                            name = "execution.isolation.thread.timeoutInMilliseconds", /*The execution.isolotion.thread.timeoutInMilliseconds is used to set the length of the timeout (in milliseconds) of the circuit braker.*/
                            value = "1000"
                    ),
                    @HystrixProperty(
                            name = "circuitBreaker.requestVolumeThreshold", /*Sets the minimum number of requests that must be processed within the rolling window before Hystrix willl even begin examining whether the circuit breaker will be tripped.*/
                            value = "10"
                    ),
                    @HystrixProperty(
                            name = "circuitBreaker.errorThresholdPercentage", /*The percentage of failures that must occur within the rolling window before the circuit braker is tripped*/
                            value = "75"
                    ),
                    @HystrixProperty(
                            name = "circuitBreaker.sleepWindowInMilliseconds", /*The number of milliseconds Hystrix will wait before trying a service call after the circuit breaker has been tripped. Note: This value can only be set with the commondPoolProperties attribute.*/
                            value = "7000"
                    ),
                    @HystrixProperty(
                            name = "metrics.rollingStats.timeInMilliseconds", /*The number of milliseconds Hystrix will collect and manitor statistics about service calls within a window*/
                            value = "15000"
                    ),
                    @HystrixProperty(
                            name = "metrics.rollingStats.numBuckets", /*The number of metrics buckets Hystrix will maintain within is manitoring window. The more buckets within the manitoring window, the lower the level of timr Hystrix will manitor for faults within the window.*/
                            value = "5"
                    )
            }
    )
    public List<License> getLicensesByOrg(String organizationId) {

        logger.debug("LicenseService.getLicensesByOrg  Correlation id: {}", UserContextHolder.getContext().getCorrelationId());

        randomlyRunLong();
        return licenseRepository.findByOrganizationId(organizationId);
    }

    /*
    * In the fallback method you return a hard-coded value.
    * */
    private List<License> buildFallbackLicenseList(String organizationId) {
        List<License> fallbackList = new ArrayList<>();
        License license = new License()
                .withId("000000-00-00000")
                .withOrganizationId(organizationId)
                .withProductName("Sorry no licensing information currently available");
        fallbackList.add(license);
        return fallbackList;
    }

    public License getLicense(String organizationId, String licenseId, String clientType) {
        License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);

        Organization org = retrieveOrgInfo(organizationId, clientType);

        return license.withOrganizationName(org.getName()).withContactName(org.getContactName()).withContactEmail(org.getContactEmail()).withContactPhone(org.getContactPhone()).withComment(config.getExampleProperty());
    }

    private Organization retrieveOrgInfo(String organizationId, String clientType) {
        Organization organization = null;

        switch (clientType) {
            case "feign":
                System.out.println("I am using the feign client");
                organization = organizationFeignClient.getOrganization(organizationId);
                break;
            case "rest":
                System.out.println("I am using the rest client");
                organization = organizationRestClient.getOrganization(organizationId);
                break;
            case "discovery":
                System.out.println("I am using the discovery client");
                organization = organizationDiscoveryClient.getOrganization(organizationId);
                break;
            default:
                organization = organizationRestClient.getOrganization(organizationId);
        }

        return organization;
    }

    /*
    * The randomlyRunLong() method gives you a one in three
    * chance of a database call running long.
    * */
    private void randomlyRunLong() {
        Random rand = new Random();
        int randomNum = rand.nextInt((3 - 1) + 1) + 1;
        if (randomNum == 3) sleep();
    }

    /*
    * You sleep for 11.000 milliseconds (11 seconds). Dfault Hystrix behaviour
    * is to time a call out after 1 second
    * */
    private void sleep() {
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void saveLicense(License license) {
        license.withId(UUID.randomUUID().toString());

        licenseRepository.save(license);

    }

    public void updateLicense(License license) {
        licenseRepository.save(license);
    }

    public void deleteLicense(License license) {
        licenseRepository.delete(license.getLicenseId());
    }

}
