package servlet;

import annotations.AnnotationController;
import annotations.JsonAnnotation;
import annotations.ParamAnnotation;
import annotations.ParamObjectAnnotation;
import utils.ModelView;
import utils.MySession;
import utils.*;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import utils.Mapping;
import utils.Function;

public class FrontController extends HttpServlet {
    private HashMap<String, Mapping> map;

    @Override
    public void init() throws ServletException {
        try {
            String packageToScan = this.getInitParameter("package");
            map = Function.getAllclazzStringAnnotation(this, packageToScan, AnnotationController.class);
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation : " + e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String url = request.getRequestURI().substring(request.getContextPath().length());
        System.out.println("Requête URI : " + url);

        if (map != null) {
            Mapping mapping = map.get(url);
            if (mapping != null) {
                try {
                    Class<?> clazz = Class.forName(mapping.getClassName());
                    String requestMethod = request.getMethod();
                    Method method = null;

                    // Find the method which get with the action and the HTTP method
                    for (VerbAction action : mapping.getVerbAction()) {
                        if (action.getVerb().equalsIgnoreCase(requestMethod)) {
                            Method[] methods = clazz.getDeclaredMethods();
                            for (Method meths : methods) {
                                if (meths.getName().equals(action.getMethodName())) {
                                    method = meths;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (method == null) {
                        throw new NoSuchMethodException(
                                "Method " + method.getName() + " not found in " + clazz.getName());
                    }

                    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                    Object[] parameterValues = Function.getParameterValue(request, method, ParamAnnotation.class,
                            ParamObjectAnnotation.class);

                    Field sessionField = null;
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.getType().equals(MySession.class)) {
                            sessionField = field;
                            break;
                        }
                    }
                    if (sessionField != null) {
                        sessionField.setAccessible(true);
                        sessionField.set(controllerInstance, new MySession(request.getSession()));
                    }

                    for (int i = 0; i < parameterValues.length; i++) {
                        if (parameterValues[i] == null && method.getParameterTypes()[i].equals(MySession.class)) {
                            MySession session = new MySession(request.getSession());
                            parameterValues[i] = session;
                        }
                    }

                    Object result = method.invoke(controllerInstance, parameterValues);

                    if (method.isAnnotationPresent(JsonAnnotation.class)) {
                        if (result instanceof ModelView) {
                            ModelView modelView = (ModelView) result;
                            RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
                            HashMap<String, Object> data = modelView.getData();
                            Gson gson = new Gson();
                            String jsonResponse = gson.toJson(data);
                            out.println(jsonResponse);
                        } else {
                            Gson gson = new Gson();
                            String jsonResponse = gson.toJson(result);
                            out.println(jsonResponse);
                        }
                    } else if (result instanceof ModelView) {
                        ModelView modelView = (ModelView) result;
                        RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
                        HashMap<String, Object> data = modelView.getData();
                        for (String key : data.keySet()) {
                            request.setAttribute(key, data.get(key));
                            System.out.println(key + ": " + data.get(key));
                        }
                        dispatcher.forward(request, response);
                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        out.println("Return type not found, neither a String or ModelView");
                    }
                } catch (Exception e) {
                    out.println("Error during the execution of the method : " + e.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No mapping found for the URL : " + url);
            }
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Map is null, the initialization might have gone wrong");
        }
    }
}