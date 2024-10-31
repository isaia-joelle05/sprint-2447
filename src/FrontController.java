package servlet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.MultipartConfig;

import annotations.AnnotationController;
import annotations.MappingAnnotation;
import annotations.ParamAnnotation;
import annotations.ParamObjectAnnotation;
import annotations.JsonAnnotation;

import com.google.gson.Gson;

import java.lang.reflect.*;

import utils.*;

@MultipartConfig
public class FrontController extends HttpServlet {
    private List<String> controllers;
    private HashMap<String, Mapping> map;

    @Override
    public void init() throws ServletException {
        String packageToScan = this.getInitParameter("package");
        try {
            this.controllers = new Function().getAllclazzsStringAnnotation(packageToScan, AnnotationController.class);
            this.map = new Function().scanControllersMethods(this.controllers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String path = new Function().getURIWithoutContextPath(request);

        if (path.contains("?")) {
            int index = path.indexOf("?");
            path = path.substring(0, index);
        }

        if (map.containsKey(path)) {
            Mapping m = map.get(path);
            try {
                Class<?> clazz = Class.forName(m.getClassName());
                Method targetMethod = null;

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(m.getMethodName())) {
                        targetMethod = method;
                        break;
                    }
                }

                if (targetMethod == null) {
                    out.println("Méthode non trouvée : " + m.getMethodName());
                    return;
                }

                Object controllerInstance = clazz.newInstance();

                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getType().equals(MySession.class)) {
                        field.setAccessible(true);
                        field.set(controllerInstance, new MySession(request.getSession()));
                    }
                }

                Object result;
                if (targetMethod.getParameterCount() == 1 && targetMethod.getParameterTypes()[0] == FilePart.class) {
                    Map<String, FilePart> fileDataMap = MultipartParser.parseMultipartRequest(request);
                    FilePart filePart = fileDataMap.get("file"); 
                    result = targetMethod.invoke(controllerInstance, filePart);
                } else {
                    result = targetMethod.invoke(controllerInstance);
                }

                if (targetMethod.isAnnotationPresent(JsonAnnotation.class)) {
                    response.setContentType("application/json");
                    Gson gson = new Gson();
                    if (result instanceof ModelView) {
                        ModelView modelView = (ModelView) result;
                        out.println(gson.toJson(modelView.getData()));
                    } else {
                        out.println(gson.toJson(result));
                    }
                } else if (result instanceof ModelView) {
                    ModelView modelView = (ModelView) result;
                    String destinationUrl = modelView.getUrl();
                    for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    RequestDispatcher dispatcher = request.getRequestDispatcher(destinationUrl);
                    dispatcher.forward(request, response);
                } else {
                    out.println("Erreur: Type de retour non géré (ni ModelView ni JSON)");
                }
            } catch (Exception e) {
                out.println("Erreur lors de l'exécution de la méthode : " + e.getMessage());
            }
        } else {
            out.println("404 NOT FOUND");
        }
    }

}
