package servlet;

import annotations.AnnotationController;
import annotations.ParamAnnotation;
import annotations.ParamObjectAnnotation;
import utils.ModelView;
import utils.MySession;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import utils.Mapping;
import utils.Function;

public class FrontController extends HttpServlet {
    private List<String> controllers;
    private HashMap<String, Mapping> map;

    @Override
    public void init() throws ServletException {
        String packageToScan = this.getInitParameter("package");
        try {
            this.controllers = new Function().getAllclazzStringAnnotation(packageToScan, AnnotationController.class);
            this.map = new Function().ControllersMethodScanning(this.controllers);
        } catch (Exception e) {
            e.printStackTrace();
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
        PrintWriter out = response.getWriter();
        String path = new Function().getURLInsideMap(request);

        if (path.contains("?")) {
            int index = path.indexOf("?");
            path = path.substring(0, index);
        }

        if (map.containsKey(path)) {
            Mapping mapp = map.get(path);
            try {
                Class<?> clazz = Class.forName(mapp.getClassName());
                Method[] methods = clazz.getDeclaredMethods();
                Method targetMethod = null;

                for (Method method : methods) {
                    if (method.getName().equals(mapp.getMethodName())) {
                        targetMethod = method;
                        break;
                    }
                }

                if (targetMethod != null) {
                    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getType().equals(MySession.class)) {
                            field.setAccessible(true);
                            field.set(controllerInstance, new MySession(request.getSession()));
                        }
                    }

                    Object[] params = Function.getParameterValue(request, targetMethod, ParamAnnotation.class,
                            ParamObjectAnnotation.class);
                    Object result_of_the_method = targetMethod.invoke(controllerInstance, params);

                    if (result_of_the_method instanceof String) {
                        out.println("The result of the execution of the method " + " " + mapp.getMethodName()
                                + " " + "is : " + " " + result_of_the_method);
                    } else if (result_of_the_method instanceof ModelView) {
                        ModelView modelView = (ModelView) result_of_the_method;
                        String destinationUrl = modelView.getUrl();
                        HashMap<String, Object> data = modelView.getData();
                        for (String key : data.keySet()) {
                            request.setAttribute(key, data.get(key));
                        }
                        RequestDispatcher dispatcher = request.getRequestDispatcher(destinationUrl);
                        dispatcher.forward(request, response);
                    } else {
                        out.println("Return type not found, neither a String or ModelView");
                    }
                } else {
                    out.println("Method not found : " + mapp.getMethodName());
                }

            } catch (Exception e) {
                e.printStackTrace();
                out.println("Error during the execution of the method : " + e.getMessage());
            }
        } else {
            out.print("\n");
            out.println("404 NOT FOUND");
        }
    }
}
