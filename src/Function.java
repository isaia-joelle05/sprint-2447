package utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import annotations.AnnotationController;
import annotations.MappingAnnotation;
import annotations.MappingAnnotation;

public class Function {
    boolean isController(Class<?> c) {
        return c.isAnnotationPresent(AnnotationController.class);
    }

    public String getURLInsideMap(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public List<String> getAllclazzStringAnnotation(String packageName,
            Class<? extends java.lang.annotation.Annotation> annotation) throws Exception {
        List<String> res = new ArrayList<>();
        // root package
        String path = this.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        File packageDir = new File(decodedPath);

        // browse all the files inside the package repository
        File[] files = packageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (clazz.getPackage() == null) {
                        throw new Exception("The class " + className + " is not inside a package.");
                    }
                    if (clazz.isAnnotationPresent(annotation)) {
                        res.add(clazz.getName());
                    }
                }
            }
        }
        return res;
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
                        res.put(url, new Mapping(c, method.getName()));
                    }
                }
            }
        }
        return res;
    }

}
