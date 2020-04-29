package com.design.pattern.proxy.dynamic1;

import jdk.nashorn.internal.ir.ReturnNode;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GPProxy {
    public static final String ln = "\r\n";

    public static Object newProxyInstance(GPClassLoader classLoader, Class<?>[] interfaces, GPInvocationHandler h) {
        try {
            String src = generateSrc(interfaces);
            System.out.println(src);
            // java 文件输出磁盘
            String filePath = GPProxy.class.getResource(".").getPath();
            File f = new File(filePath + "$Proxy0.java");
            FileWriter fw = new FileWriter(f);
            fw.write(src);
            fw.flush();
            fw.close();


            // 把生成的.java编译成.class 文件
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();//
            StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);
            Iterable iterable = manager.getJavaFileObjects(f);
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, null, null, iterable);
            task.call();
            manager.close();
            // 把编译生成的.class 文件加载到JVM中
            Class proxyClass = classLoader.findClass("$Proxy0");
            Constructor c = proxyClass.getConstructor(GPInvocationHandler.class);
            //f.delete();
            // 把字节码重组以后的新的代理对象
            return c.newInstance(h);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String generateSrc(Class<?>[] interfaces) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.design.pattern.proxy.dynamic1;" + ln);
        sb.append("import com.design.pattern.proxy.statics.Person;" + ln);
        sb.append("import java.lang.reflect.*;" + ln);
        sb.append("import java.lang.reflect.UndeclaredThrowableException;" + ln);
        sb.append("public class $Proxy0 implements " + interfaces[0].getName() + " { " + ln);
        sb.append(" GPInvocationHandler h; " + ln);
        sb.append(" public $Proxy0(GPInvocationHandler h) { " + ln);
        sb.append("this.h = h ;" + ln);
        sb.append(" } " + ln);
        for (Method m : interfaces[0].getMethods()) {
            Class<?> params[] = m.getParameterTypes();
            StringBuilder paramNames = new StringBuilder();
            StringBuilder paramValues = new StringBuilder();
            StringBuilder paramClasses = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                Class clazz = params[i];
                String type = clazz.getName();
                paramNames.append(type + " " + "index" + i).append(",");
                paramValues.append("index" + i).append(",");
                paramClasses.append(clazz.getName() + ".class").append(",");
            }

            String paramName = paramNames.substring(0,paramNames.length()-1);
            String paramValue = paramValues.substring(0,paramValues.length() -1 );
            String paramClasse = paramClasses.substring(0,paramClasses.length() -1);


            sb.append("public " + m.getReturnType().getName() + " " + m.getName() + " (" +
                    paramName.toString() + " ) { " + ln);
            sb.append("try {" + ln);
            sb.append("Method m = " + interfaces[0].getName() + ".class.getMethod(\"" + m.getName() + "\",new Class[] { " + paramClasse.toString() + "});" + ln);
            sb.append((hasReturnValue(m.getReturnType()) ? "return " : "") + getCaseCode("this.h.invoke(this,m,new Object[]{  " + paramValue + "})", m.getReturnType()) + " ;" + ln);
            sb.append(" } catch (Throwable e) {");
            sb.append("}");
            sb.append(getReturnEmptyCode(m.getReturnType()));
            sb.append("}");
        }
        sb.append("}" + ln);
        return sb.toString();
    }

    private static Map<Class, Class> mappings = new HashMap<Class, Class>();

    static {
        mappings.put(int.class, Integer.class);
        mappings.put(String.class,String.class);
    }

    private static String getReturnEmptyCode(Class<?> returnClass) {
        if (mappings.containsKey(returnClass) && "int".equals(returnClass.getName()) ) {
            return "return 0;";
        } else if (returnClass == void.class) {
            return "";
        } else {
            return "return null;";
        }
    }

    private static String getCaseCode(String code, Class<?> returnClass) {
        if (mappings.containsKey(returnClass) && "int".equals(returnClass.getName())) {
            return "((" + mappings.get(returnClass).getName() + ")" + code + ")." + returnClass.getSimpleName() + "Value()";
        }else if("java.lang.String".equals(returnClass.getName())){
            return "((" + mappings.get(returnClass).getName() + ")" + code + ")";
        }
        return code;
    }


    private static boolean hasReturnValue(Class<?> clazz) {
        return clazz != void.class;
    }


    public static String toLowerFirstCase(String src) {
        char[] chars = src.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
