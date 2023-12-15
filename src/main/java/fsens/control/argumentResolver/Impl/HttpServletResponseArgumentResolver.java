package fsens.control.argumentResolver.Impl;

import fsens.control.argumentResolver.ArgumentResolver;
import fsens.control.annotation.*;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;


@argumentResolver
public class HttpServletResponseArgumentResolver implements ArgumentResolver {
    public boolean support(Class<?> type, int paramIndex, Method method){
        return ServletResponse.class.isAssignableFrom(type);
    }


    public Object argumentResolver(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Class<?> type,
                                   int paramIndex,
                                   Method method){
        return response;
    }
}
