package utils;

import java.lang.reflect.Method;

public class Mapping {
    String className;
    String methodName;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Mapping() {
    }

    public Mapping(String className, String methodName) {
        setClassName(className);
        setMethodName(methodName);
    }

    public Object invokeMethod() throws Exception {
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName);
        Object in_stance = clazz.getDeclaredConstructor().newInstance();
        return method.invoke(in_stance);
    }

}
