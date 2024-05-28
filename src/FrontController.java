package servlet;

import annotations.AnnotationController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

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
        StringBuffer url = request.getRequestURL();
        // URL to search inside the map
        String path = new Function().getURLInsideMap(request);
        out.println("URL inside the map: " + path);

        // Taking the mapping according to the url
        if (map.containsKey(path)) {

            Mapping mapp = map.get(path);
            out.print("\n");
            out.println("The method inside the class " + mapp.getClassName() + " " + "is" + " " + mapp.getMethodName());

            try {
                Object result_of_the_method = mapp.invokeMethod();
                out.println("The result of the execution of the method " + " " + mapp.getMethodName()
                        + " " + "is : ");
                out.println(result_of_the_method);

            } catch (Exception e) {
                out.println("Error during the execution of the method : " + e.getMessage());
                e.printStackTrace(out);
            }
        } else {
            out.print("\n");
            out.println("No method found in this url");
        }
        // show the controllers
        out.print("\n");
        out.println("Here are all of your controllers : ");
        for (String class1 : this.controllers) {
            out.println(class1); /* print the controllers */
        }
    }
}