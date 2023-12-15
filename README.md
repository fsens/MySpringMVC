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

![image-20231215123624758](https://github.com/fsens/MySpringMVC/assets/95872817/4760c27c-e1e0-43e5-97c2-bf625a6c6f5e)

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

# 三、实现效果

## 项目结构

![image-20231215125147788](https://github.com/fsens/MySpringMVC/assets/95872817/704d1860-18fd-4d83-99ad-b488008f375a)


## 编写用于测试的后端代码

Controller：

```java
@myController
@myRequestMapping("/fsens")
public class TestController {

    //指定注入queryService实例
    @myQualifier("queryService")
    private MyService queryService;

    @myRequestMapping("/query")
    public void queryInfo(HttpServletRequest request, HttpServletResponse response,
                      @myRequestParam(value = "info")String info){
        try {
            PrintWriter pw = response.getWriter();
            String result = queryService.query(info);
            pw.write(result);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

}
```

Service：

```Java
@myService
public class queryService implements MyService {
    public String query(String info){
        if(info.equalsIgnoreCase( "name")){
            return new User().getName();
        }
        else if(info.equalsIgnoreCase("sex")){
            return new User().getSex();
        }
        else if(info.equalsIgnoreCase("age")){
            return new User().getAge();
        }
        return null;
    }

}
```

用于测试数据User：

```Java
public class User {
    private String name = "fsens";

    private String sex = "man";

    private String age = "20";

    public User(){

    }

    public User(String name, String sex, String age){
        this.name = name;
        this.sex = sex;
        this.age = age;
    }

    public String getName(){
        return this.name;
    }

    public String getSex(){
        return this.sex;
    }

    public String getAge(){
        return this.age;
    }


}
```

## 效果

起始页面：

![image-20231215125520821](https://github.com/fsens/MySpringMVC/assets/95872817/1f8d862c-c547-48fc-9e72-3404f64ed645)


默认url必须输入参数：

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface myRequestParam {
    String value() default "";
    boolean required() default true;
}
```

若未输入参数：

![image-20231215124721731](https://github.com/fsens/MySpringMVC/assets/95872817/cde99e8f-7288-40e3-a72a-342b23a3de23)


输入参数：

![image-20231215124751033](https://github.com/fsens/MySpringMVC/assets/95872817/003d4212-f4de-4e12-ae6e-9457b3b7f9be)
