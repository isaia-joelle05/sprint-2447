package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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

import utils.Mapping;
import utils.Function;
import utils.ModelView;
import utils.MySession;
import utils.*;

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
            e.getMessage();
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
        response.setContentType("text/html;charset=UTF-8");

        String path = new Function().getURIWithoutContextPath(request);
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }

        // Check if the requested path exists in the map
        if (!map.containsKey(path)) {
            out.println("The map contains nothing: 404 NOT FOUND");
            return;
        }

        Mapping m = map.get(path);
        try {
            Class<?> clazz = Class.forName(m.getClassName());
            Method targetMethod = null;

            // Find the method in the class that matches the mapping
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

            Object[] params = Function.getParameterValue(request, response, targetMethod, ParamAnnotation.class, ParamObjectAnnotation.class);
            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

            // Initialize MySession if required by the controller
            for (Field field : clazz.getDeclaredFields()) {
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
                result = targetMethod.invoke(controllerInstance, params);
            }

            handleResponse(result, targetMethod, request, response, out);

        } catch (Exception e) {
            e.printStackTrace(out);
            out.println("Erreur lors de l'exécution de la méthode : " + e.getMessage());
        }
    }

    private void handleResponse(Object result, Method targetMethod, HttpServletRequest request,
                                HttpServletResponse response, PrintWriter out) throws IOException, ServletException {
        if (result instanceof FileExport) {
            FileExport export = (FileExport) result;
            response.reset(); 
            response.setContentType(export.getFileType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + export.getFileName() + "\"");
            response.getOutputStream().write(export.getContent());
            response.getOutputStream().flush();
        }else if (targetMethod.isAnnotationPresent(JsonAnnotation.class)) {
            response.reset();
            response.setContentType("application/json");
            Gson gson = new Gson();
            if (result instanceof ModelView) {
                ModelView modelView = (ModelView) result;
                out.println(gson.toJson(modelView.getData()));
            } else {
                out.println(gson.toJson(result));
            }
            out.flush();
        } else if (result instanceof String) {
            response.reset();
            response.setContentType("text/html");
            out = response.getWriter();
            out.println("Resultat de l'execution de la méthode " + targetMethod.getName() + " est " + result);
            out.flush();
        } else if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            String destinationUrl = modelView.getUrl();
            HashMap<String, Object> data = modelView.getData();
            for (String key : data.keySet()) {
                request.setAttribute(key, data.get(key));
            }
            RequestDispatcher dispatcher = request.getRequestDispatcher(destinationUrl);
            dispatcher.forward(request, response);
        } else {
            response.reset();
            response.setContentType("text/html");
            out.println("Le type de retour n'est ni un String ni un ModelView");
            out.flush();
        }
    }

    private void handleError(HttpServletResponse response, Exception e) throws IOException {
        response.reset();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        e.printStackTrace(out);
        out.print("Erreur lors de l'exécution de la méthode : " + e.getMessage());
        out.flush();
    }
}
