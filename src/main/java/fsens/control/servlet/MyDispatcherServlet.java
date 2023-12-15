package fsens.control.servlet;

import fsens.control.annotation.*;
import fsens.control.handler.MethodHandler;
import fsens.control.handlerAdapter.HandlerAdapter;
import fsens.control.handlerAdapter.Impl.MyHandlerAdapter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * MyDispatcherServlet本质上也是一个servlet;继承HttpServlet,实现更丰富的功能
 */
public class MyDispatcherServlet extends HttpServlet {
    //创建装className的容器
    private List<String> classNames = new ArrayList<String>();
    //创建bean容器
    private Map<String, Object> beans = new HashMap<String, Object>();
    //创建handlerMapping
    private List<MethodHandler> handlerMapping = new ArrayList<MethodHandler>();

    /**
     * @see javax.servlet.Servlet#init(ServletConfig)
     */
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

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse reponse)
     * @param request
     * @param response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {
        this.doPost(request, response);
    }


    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse reponse)
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException{

        doDispatch(request, response);

    }

    /**
     *
     * @param request
     * @param response
     * @throws Exception
     */
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
        //得到handler
        MethodHandler handler = getHandler(request);
        //若未找到，的报404 not found!
        if(handler == null){
            try {
                response.getWriter().write("404 not found!");
            }
           catch (IOException e){
                e.printStackTrace();
           }
        }

        //得到HandlerAdapter
        HandlerAdapter ha = new MyHandlerAdapter();
        //执行对应的controller
        ha.hand(request, response, handler, beans);

    }


    private void scanPackage(String basePackageName){
        //得到基包的url
        URL url = this.getClass().getClassLoader().getResource("/" + replaceTo(basePackageName));
        //得到基包名（全名）
        String fileStr = url.getFile();
        //创建基包这一文件对象
        File file = new File(fileStr);
        //得到基包下所有与之直接是父子关系的文件和目录（部分名）
        String[] filesStr = file.list();

        for(String name : filesStr){
            File filePath = new File(fileStr + name);

            //如果是目录，就再次进行扫包
            if(filePath.isDirectory()){
                scanPackage(basePackageName + "." + name);
            }
            else {
                //filePath其实是很长的(全名),getName()删去了前缀，即得到xxx.class
                //最终得到诸如fsens.control.annotation.xxx.class的classNames
                classNames.add(basePackageName + "." + filePath.getName());
            }
        }
    }

    private String replaceTo(String packageName){
        //将"."转换成"/"
        return packageName.replaceAll("\\.", "/");
    }


    private void instantiate(){
        if(classNames.size() == 0){
            System.out.println("包扫描失败：classNames长度为0");
            return;
        }
        for(String className : classNames){
            //得到.class后缀去掉的新类名
            String cn = className.replace(".class", "");

            try{
                //cn得是全类名
                Class clazz = Class.forName(cn);

                //将注解了myController或者myRequestMapping或者myService实例化
                if(clazz.isAnnotationPresent(myController.class)){
                    myRequestMapping myrequestmapping = (myRequestMapping) clazz.getAnnotation(myRequestMapping.class);
                    String rmValue = myrequestmapping.value();
                    // 检查@myRequestMapping注解值是否未指定，如果为指定则抛出异常
                    if (rmValue.equals("")){
                        throw new RuntimeException("注解了@myRequestMapping的类未指定url");
                    }
                    // 检查是否存在相同的url，如果是则抛出异常
                    if (beans.containsKey(rmValue)){
                        throw new RuntimeException("注解了@myRequestMapping的类存在相同的url");
                    }

                    Object instance = clazz.newInstance();
                    beans.put(rmValue, instance);
                }
                else if(clazz.isAnnotationPresent(myService.class)){
                    myService myservice = (myService) clazz.getAnnotation(myService.class);
                    String serviceValue = myservice.value();
                    // 实现@myService默认值为所在类的首字母为小写的类名的功能
                    if (serviceValue.equals("")){
                        serviceValue = clazz.getSimpleName();
                        //首字母小写
                        serviceValue = Character.toLowerCase(serviceValue.charAt(0)) + serviceValue.substring(1);
                    }
                    // 检查是否存在相同的service名，如果是则抛出异常
                    if (beans.containsKey(serviceValue)){
                        throw new RuntimeException("有相同的service名称");
                    }

                    Object instance = clazz.newInstance();
                    beans.put(serviceValue, instance);
                }
                else if(clazz.isAnnotationPresent(argumentResolver.class)){
                    argumentResolver argumentResolver = (argumentResolver) clazz.getAnnotation(argumentResolver.class);
                    String argumentResolverValue = argumentResolver.value();
                    // 实现@argumentResolver默认值为所在类的首字母为小写的类名的功能
                    if (argumentResolverValue.equals("")){
                        argumentResolverValue = clazz.getSimpleName();
                        //首字母小写
                        argumentResolverValue = Character.toLowerCase(argumentResolverValue.charAt(0)) + argumentResolverValue.substring(1);
                    }
                    // 检查是否存在相同的argumentResolver名，如果是则抛出异常
                    if (beans.containsKey(argumentResolverValue)){
                        throw new RuntimeException("有相同的argumentResolver名称");
                    }

                    Object instance = clazz.newInstance();
                    beans.put(argumentResolverValue, instance);
                }
                else{
                    continue;
                }

            }
            catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            catch (InstantiationException e){//得带无参构造器，否则系统不能实例化
                e.printStackTrace();
            }
            catch (IllegalAccessException e){//由不同的反射调用方法获取到不同访问权限的成员，错误的访问方式会引发该异常
                e.printStackTrace();
            }


        }
    }

    private void ioc(){
        if(beans.size() == 0){
            System.out.println("实例化未成功：beans长度为0");
            return;
        }

        //遍历beans,查找注解了myController的bean
        for(Map.Entry<String, Object> entry : beans.entrySet()){
            Object instance = entry.getValue();
            Class clazz = instance.getClass();

            if(clazz.isAnnotationPresent(myController.class)){
                Field[] fields = clazz.getDeclaredFields();//获取所有字段，无论什么访问修饰符

                //遍历该类的字段,查找注解了myQualifier的字段,并对其注入实例
                for(Field field : fields){
                    if(field.isAnnotationPresent(myQualifier.class)){
                        myQualifier myqualifier = (myQualifier) field.getAnnotation(myQualifier.class);
                        String value = myqualifier.value();
                        // 检查@myQualifier是否指定了要注入的bean名称，若否，则抛出异常
                        if (value.equals("")){
                            throw new RuntimeException("@myQualifier未指定需要的bean名称");
                        }

                        field.setAccessible(true);//使可以访问字段，即使是私有字段
                        try {
                            field.set(instance, beans.get(value));
                        }
                        catch (IllegalArgumentException e){
                            e.printStackTrace();
                        }
                        catch (IllegalAccessException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


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


    private MethodHandler getHandler(HttpServletRequest request) {
        //得到uri
        //    /MySpringmvc/fsens/query
        String uri = request.getRequestURI();
        //得到上下文
        //    /MySpringmvc
        String context = request.getContextPath();
        //得到匹配对应handler的路径
        String path = uri.replace(context, "");

        for (MethodHandler handler : handlerMapping) {
            if (handler.isSupport(path)){
                return handler;
            }
        }
        return null;
    }
}


