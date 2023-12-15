# 一、项目简介

这是一个借鉴SpringMVC实现的一个简易的MVCdemo，它实现了简单的Ioc功能。目前实现了以下功能：

## 1.MySpringMVC配置和初始化

1.完成了MyDispatcherServlet作为总控制器，需要在web.xml中配置它。

```xml
<servlet>
  <servlet-name>MyDispatcherServlet</servlet-name>
  <display-name>MyDispatcherServlet</display-name>
  <description></description>
  <servlet-class>fsens.control.servlet.MyDispatcherServlet</servlet-class>
  <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
  <servlet-name>MyDispatcherServlet</servlet-name>
  <url-pattern>/</url-pattern>
</servlet-mapping>
```

2.完成了Ioc容器的初始化功能。

```java
public void init(ServletConfig config) throws ServletException{
    //1.我们要根据一个基本包进行扫描，扫描里面的子包以及子包下的类
    scanPackage("fsens.control");
    //2、把扫描出来的类进行实例化
    //beans只有@Controller和@Service标注了的实例
    instantiate();
    //3、依赖注入，把service层的实例注入到controller
    ioc();
    //4、初始化handlerMapping
    initHandlerMapping();
}
```

## 2.注解功能

它们都能实现bean名称唯一性判断。

- @myController：标明该Component是Controller，默认名称为""；

- @myRequestMapping：标明Controller处理的url。必须指定url，且不同Controller不能出现相同的url；

- @myService：标明这是一个服务类，是一个普通的Component。默认名称是注解的类的的名称，首字母换成了小写的；

- @myQualifier：向该字段注入指定bean名称的实例。必须指定bean名称。

- @myRequestParam：获取url中?后的参数并注入到指定的形参。可以设置在url中是否必须指定参数。

# 二、项目整体架构

## 1.主要流程

![image-20231215123624758](https://github.com/fsens/MySpringMVC/assets/95872817/d7ae8c7e-420d-4169-8fa4-52dbee00f61f)


## 2.主要数据结构

1.存放beans的Map：

```java
private Map<String, Object> beans = new HashMap<String, Object>();
```

2.HandlerMapping：

​		Handler：

```Java
public class MethodHandler {


   private String url;


   private Method method;


   public MethodHandler(String url, Method method){
       this.url = url;
       this.method = method;
   }


   public void setMethod(Method method){
       this.method = method;
   }


   public Method getMethod(){
       return method;
   }


   public String getUrl(){
       return this.url;
   }


   public boolean isSupport(String url){

       if(url.equals(this.url)){
           return true;
       }
       return false;
   }


}
```

​		HandlerMapping：

```java
private List<MethodHandler> handlerMapping = new ArrayList<MethodHandler>();
```

## 3.关键代码

1.初始化HandlerMapping：initHandlerMapping()

```java
private void initHandlerMapping(){
    if(beans.entrySet().size() == 0){
        System.out.println("实例化失败：beans.entrySet()长度为0");
        return;
    }

    //遍历beans,查找myRequestMapping注解的bean
    for(Map.Entry<String, Object> entry : beans.entrySet()){
        Object instance = entry.getValue();
        Class clazz = instance.getClass();

        if(clazz.isAnnotationPresent(myRequestMapping.class)){
            myRequestMapping myrequestmapping_class = (myRequestMapping) clazz.getAnnotation(myRequestMapping.class);
            //bean上的url
            String classUrl = myrequestmapping_class.value();

            Method[] methods = clazz.getDeclaredMethods();

            //遍历该bean的方法,查找注解了myRequestMapping的方法
            for (Method method : methods){
                if(method.isAnnotationPresent(myRequestMapping.class)){
                    myRequestMapping myrequestmapping_method  = method.getAnnotation(myRequestMapping.class);
                    //method上的url
                    String methodUrl = myrequestmapping_method.value();

                    //合并成完整的url
                    String url = classUrl + methodUrl;

                    MethodHandler handler = new MethodHandler(url, method);
                    handlerMapping.add(handler);
                }
            }
        }
    }
}
```

2.HandlerAdapter：hand(...)：

```java
public void hand(HttpServletRequest request,
                 HttpServletResponse response,
                 MethodHandler handler,
                 Map<String, Object> beans) {
    //得到实参
    Object[] args = argumentResolver(request, response, handler, beans);
    //如果实参里面有任意一个解析不正确，则返回
    if(args == null)  return;
    //得到具体的方法
    Method method = handler.getMethod();
    //得到method的路径
    String path = handler.getUrl();
    //得到method所在的类
    Object instance = beans.get("/" + path.split("/")[1]);
    try {
        //调用反射API执行controller
        method.invoke(instance, args);
    } catch (IllegalAccessException e) {
        e.printStackTrace();
    } catch (IllegalArgumentException e) {
        e.printStackTrace();
    } catch (InvocationTargetException e) {
        e.printStackTrace();
    }
}
```
