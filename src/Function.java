package utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServlet;

import annotations.AnnotationController;
import annotations.MappingAnnotation;
import annotations.ParamAnnotation;
import annotations.ParamObjectAnnotation;
import annotations.*;
import utils.MySession;

public class Function {
    boolean isController(Class<?> c) {
        return c.isAnnotationPresent(AnnotationController.class);
    }

    public String getURLInsideMap(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    // public List<String> getAllclazzStringAnnotation(String packageName,
    //         Class<? extends java.lang.annotation.Annotation> annotation) throws Exception {
    //     List<String> res = new ArrayList<>();
    //     // root package
    //     String path = this.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
    //     String decodedPath = URLDecoder.decode(path, "UTF-8");
    //     File packageDir = new File(decodedPath);

    //     // browse all the files inside the package repository
    //     File[] files = packageDir.listFiles();
    //     if (files != null) {
    //         for (File file : files) {
    //             if (file.isFile() && file.getName().endsWith(".class")) {
    //                 String className = packageName + "." + file.getName().replace(".class", "");
    //                 Class<?> clazz = Class.forName(className);
    //                 if (clazz.getPackage() == null) {
    //                     throw new Exception("The class " + className + " is not inside a package.");
    //                 }
    //                 if (clazz.isAnnotationPresent(annotation)) {
    //                     res.add(clazz.getName());
    //                 }
    //             }
    //         }
    //     }
    //     return res;
    // }

    public static HashMap<String, Mapping> getAllclazzStringAnnotation(HttpServlet servlet, String packageName, Class<? extends java.lang.annotation.Annotation> annotation) throws Exception{
        HashMap<String, Mapping> map = new HashMap<>();
        try {
            String path = servlet.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File packageDir = new File(decodedPath);

            if(!packageDir.exists() || !packageDir.isDirectory()) {
                throw new Exception("The package " + packageName + "does not exist");
            }

            File[] files = packageDir.listFiles();
            if(files != null){
                for (File file : files) {
                    if(file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageName + "." + file.getName().replace(".class", "");
                        Class<?> clazz = Class.forName(className);

                        if(clazz.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class))) {
                            Method[] methods = clazz.getDeclaredMethods();

                            for(Method m : methods) {
                                if(m.isAnnotationPresent(MappingAnnotation.class)) {
                                    MappingAnnotation urlAnnotation = m.getAnnotation(MappingAnnotation.class);
                                    String url = urlAnnotation.url();

                                    if(!map.containsKey(url)) {
                                        map.put(url, new Mapping(clazz.getName()));
                                    }

                                    boolean isGet = m.isAnnotationPresent(Get.class);
                                    boolean isPost = m.isAnnotationPresent(Post.class);
                                    if (!isGet && !isPost) {
                                        isGet = true;
                                    }

                                    String verb = null;
                                    if (isGet) {
                                        verb = "GET";
                                    } else {
                                        verb = "POST";
                                    }
                                    map.get(url).addVerbAction(m.getName(), verb);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }



    public HashMap<String, Mapping> ControllersMethodScanning(List<String> controllers) throws Exception {
        HashMap<String, Mapping> res = new HashMap<>();
        HashMap<String, String> urlMap = new HashMap<>();

        for (String c : controllers) {
            Class<?> clazz = Class.forName(c);
            // get all the methods inside the class
            Method[] meths = clazz.getDeclaredMethods();
            for (Method method : meths) {
                if (method.isAnnotationPresent(MappingAnnotation.class)) {
                    String url = method.getAnnotation(MappingAnnotation.class).url();
                    if (urlMap.containsKey(url)) {
                        String method_present = urlMap.get(url);
                        String new_method = clazz.getName() + ":" + method.getName();
                        throw new Exception("The url " + url + " is already mapped on " + method_present
                                + " and cannot be mapped on " + new_method + " anymore");

                    } else {
                        // Si l'URL n'est pas déjà présente, l'ajouter à la map
                        urlMap.put(url, clazz.getName() + ":" + method.getName());
                        // get the annotation
                        res.put(url, new Mapping(method.getName()));
                    }
                }
            }
        }
        return res;
    }

    public static Object convertParameterValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type == char.class || type == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException("Invalid character value:" + value);
            }
            return value.charAt(0);
        }
        return null;
    }

    public static Object[] getParameterValue(HttpServletRequest request, Method method,
            Class<ParamAnnotation> annotationClass,
            Class<ParamObjectAnnotation> paramObjectAnnotationClass) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            String paramName;
            if (parameters[i].getType().equals(MySession.class)) {
                parameterValues[i] = new MySession(request.getSession());
            } else if (parameters[i].isAnnotationPresent(annotationClass)) {
                ParamAnnotation param = parameters[i].getAnnotation(annotationClass);
                paramName = param.value();
                String paramValue = request.getParameter(paramName);
                System.out.println("Parameter: " + paramName + " = " + paramValue);
                parameterValues[i] = convertParameterValue(paramValue, parameters[i].getType());
            } else if (parameters[i].isAnnotationPresent(paramObjectAnnotationClass)) {
                ParamObjectAnnotation paramObject = parameters[i].getAnnotation(paramObjectAnnotationClass);
                String objectName = paramObject.objectName();
                try {
                    Object paramObjectInstance = parameters[i].getType().getDeclaredConstructor().newInstance();
                    Field[] fields = parameters[i].getType().getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(objectName + "." + fieldName);
                        System.out.println("Field: " + objectName + "." + fieldName + " = " + paramValue);
                        if (paramValue != null) {
                            field.setAccessible(true);
                            field.set(paramObjectInstance, convertParameterValue(paramValue, field.getType()));
                        }
                    }
                    parameterValues[i] = paramObjectInstance;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create and populate parameter object: " + e.getMessage());
                }
            } else {
                throw new Exception("ETU002447 : There is no annotation parameter in your function");
            }
        }
        return parameterValues;
    }

}
