package fsens.control.argumentResolver.Impl;

import fsens.control.argumentResolver.ArgumentResolver;
import fsens.control.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


@argumentResolver
public class RequestParamArgumentResolver implements ArgumentResolver {
    public boolean support(Class<?> type, int paramIndex, Method method){
        //位置：annotations[i][j],第i个参数，第j个注解实例，整体表示一个注解实例
        Annotation[][] an = method.getParameterAnnotations();

        Annotation[] paramAns = an[paramIndex];

        for (Annotation paramAn : paramAns){
            //当前Class对象如果是参数Class对象的父类，父接口，或者是相同，都会返回true。
            if(myRequestParam.class.isAssignableFrom(paramAn.getClass())){
                return true;
            }
        }
        return false;
    }


    public Object argumentResolver(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Class<?> type,
                                   int paramIndex,
                                   Method method) {
        //位置：annotations[i][j],第i个参数，第j个注解实例，整体表示一个注解实例
        Annotation[][] an = method.getParameterAnnotations();

        Annotation[] paramAns = an[paramIndex];

        for (Annotation paramAn : paramAns){
            if(myRequestParam.class.isAssignableFrom(paramAn.getClass())){
                myRequestParam mrp = (myRequestParam) paramAn;

                String value = mrp.value();//info：用户输入值的参数
                String str = request.getParameter(value);//用户输入的值
                /**
                 * 两种情况:
                 * 1.输入为空,这里得分required为true还是false来讨论
                 * 2.输入非空，分输入错误和输入正确来讨论，这时和required的值无关
                 */
                //str == null
                if(str == null){
                    //required == true
                    if(mrp.required()){
                        try {
                            response.getWriter().write("404 not found because of absence of parameter(s).");
                            return null;
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    //required == false
                    //返回""是为了防止method.invoke的参数为空。如果为空，会抛出错误
                    else{
                        return "";
                    }
                }
                //str != null
                else {
                    //参数输入正确
                    if(str.equals("name") || str.equals("sex") || str.equals("age")){
                        return str;
                    }
                    //参数输入错误
                    else {
                        try{
                            response.getWriter().write("404 not found because of wrong parameter(s).");
                            return null;
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }
}

