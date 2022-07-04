package com.thoughtmechanix.licenses.hystrix;

import com.thoughtmechanix.licenses.utils.UserContext;
import com.thoughtmechanix.licenses.utils.UserContextHolder;

import java.util.concurrent.Callable;

public final class DelegatingUserContextCallable<V> implements Callable<V> {
    private final Callable<V> delegate;
    private UserContext orginalUserContext;

    /*
    * Custom Callable class will be passed the orginal Callable class
    * that will invoke your Hystrix protected code and UserContext coming in from the parent thread
    * */
    public DelegatingUserContextCallable(Callable<V> delegate, UserContext userContext) {
        this.delegate = delegate;
        this.orginalUserContext = userContext;
    }

    /*
    * Once the UserContext is set invoke the call() method on the Hystrix protected method;
    * for instance, your LicanceServer.getLicenseByOrg() method.
    * */
    @Override
    public V call() throws Exception {
        UserContextHolder.setContext( orginalUserContext );
        try{
            return delegate.call();
        }finally {
            this.orginalUserContext = null;
        }
    }

    public static <V> Callable create(Callable<V> delegate, UserContext userContext) {
        return new DelegatingUserContextCallable<V>(delegate, userContext);
    }
}
