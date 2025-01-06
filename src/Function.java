package utils;

import java.io.File;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.*;

import annotations.AnnotationController;
import annotations.MappingAnnotation;
import annotations.ParamAnnotation;
import annotations.ParamObjectAnnotation;
import annotations.isError;
import annotations.TypeValidationAnnotation;

public class Function {
    boolean isController(Class<?> c) {
        return c.isAnnotationPresent(AnnotationController.class);
    }

    public List<String> getAllclazzsStringAnnotation(String packageName,
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
                    // Check if the class has a package
                    if (clazz.getPackage() == null) {
                        throw new Exception("La classe " + className + " n'est pas dans un package.");
                    }
                    if (clazz.isAnnotationPresent(annotation)) {
                        res.add(clazz.getName());
                    }
                }
            }
        }
        return res;
    }

    public HashMap<String, Mapping> scanControllersMethods(List<String> controllers) throws Exception {
        HashMap<String, Mapping> res = new HashMap<>();
        HashMap<String, String> urlMap = new HashMap<>(); 

        for (String c : controllers) {
            Class<?> clazz = Class.forName(c);
            Method[] meths = clazz.getDeclaredMethods();
            for (Method method : meths) {
                if (method.isAnnotationPresent(MappingAnnotation.class)) {
                    String url = method.getAnnotation(MappingAnnotation.class).url();
                    if (urlMap.containsKey(url)) {
                        String method_present = urlMap.get(url);
                        String new_method = clazz.getName() + ":" + method.getName();
                        throw new Exception("L'URL " + url + " est déjà mappée sur " + method_present
                                + " et ne peut pas être mappée sur " + new_method + " de nouveau.");
                    } else {
                        urlMap.put(url, clazz.getName() + ":" + method.getName());
                        res.put(url, new Mapping(c, method.getName()));
                    }
                }
            }
        }
        return res;
    }

    public String getURIWithoutContextPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
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

    public static Map<String, String> verifyValidation(Object obj) throws IllegalAccessException {
        Map<String, String> errors =  new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true); 

            Object value = field.get(obj);

            if (field.isAnnotationPresent(TypeValidationAnnotation.NotNull.class)) {
                TypeValidationAnnotation.NotNull notNull = field.getAnnotation(TypeValidationAnnotation.NotNull.class);
                if (value == null) {
                    errors.put(field.getName(), notNull.value());
                }
            }

            if (field.isAnnotationPresent(TypeValidationAnnotation.Min.class) && value instanceof Number) {
                TypeValidationAnnotation.Min min = field.getAnnotation(TypeValidationAnnotation.Min.class);
                if (((Number) value).intValue() < min.value()) {
                    errors.put(field.getName(), min.message());
                }
            }

            if (field.isAnnotationPresent(TypeValidationAnnotation.Max.class) && value instanceof Number) {
                TypeValidationAnnotation.Max max = field.getAnnotation(TypeValidationAnnotation.Max.class);
                if (((Number) value).intValue() > max.value()) {
                    errors.put(field.getName(), max.message());
                }
            }

            if (field.isAnnotationPresent(TypeValidationAnnotation.Pattern.class) && value != null) {
                TypeValidationAnnotation.Pattern pattern = field.getAnnotation(TypeValidationAnnotation.Pattern.class);
                if (!value.toString().matches(pattern.regex())) {
                    errors.put(field.getName(), pattern.message());
                }
            }

            if (field.isAnnotationPresent(TypeValidationAnnotation.NotEmpty.class) && value != null) {
                TypeValidationAnnotation.NotEmpty notEmpty = field.getAnnotation(TypeValidationAnnotation.NotEmpty.class);
                if (value.toString().isEmpty()) {
                    errors.put(field.getName(), notEmpty.message());
                }
            }

            if (field.isAnnotationPresent(TypeValidationAnnotation.Positive.class) && value instanceof Number) {
                TypeValidationAnnotation.Positive positive = field.getAnnotation(TypeValidationAnnotation.Positive.class);
                if (((Number) value).intValue() <= 0) {
                    errors.put(field.getName(), positive.message());
                }
            }
        }
        return errors;
    }


    public static Object[] getParameterValue(HttpServletRequest request, HttpServletResponse response, Method method,
            Class<ParamAnnotation> annotationClass,
            Class<ParamObjectAnnotation> paramObjectAnnotationClass) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getType().equals(MySession.class)) {
                parameterValues[i] = new MySession(request.getSession());
            } else if (parameters[i].isAnnotationPresent(annotationClass)) {
                ParamAnnotation param = parameters[i].getAnnotation(annotationClass);
                String paramName = param.value();
                String paramValue = request.getParameter(paramName);
                System.out.println("Parameter: " + paramName + " = " + paramValue);
                parameterValues[i] = convertParameterValue(paramValue, parameters[i].getType());
            } else if (parameters[i].isAnnotationPresent(paramObjectAnnotationClass)) {
                ParamObjectAnnotation paramObject = parameters[i].getAnnotation(paramObjectAnnotationClass);
                String objName = paramObject.objectName();
                try {
                    Object paramObjectInstance = parameters[i].getType().getDeclaredConstructor().newInstance();

                    Map<String, String> fieldsValues = new HashMap<>();
                    Map<String, String> validationErrors = new HashMap<>();

                    Field[] fields = parameters[i].getType().getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(objName + "." + fieldName);
                        System.out.println("Field: " + objName + "." + fieldName + " = " + paramValue);
                        if (paramValue != null) {
                            field.setAccessible(true);
                            field.set(paramObjectInstance, convertParameterValue(paramValue, field.getType()));
                        }
                    }
                    validationErrors = verifyValidation(paramObjectInstance);
                    if (!validationErrors.isEmpty()) {
                        request.setAttribute("errors", validationErrors);
                        request.setAttribute("values", fieldsValues);
                        
                        isError iserr = method.getAnnotation(isError.class);
                        if(iserr != null) {
                            String errUrl = iserr.url();
                            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {

                                @Override
                                public String getMethod() {
                                    return "GET";
                                }
                            };
                            RequestDispatcher dispatcher = request.getRequestDispatcher(errUrl);
                            dispatcher.forward(wrappedRequest, response);
                        }
                        System.out.println("Validation errors for object: " + objName);
                        throw new Exception("Validation errors: " + validationErrors);
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
