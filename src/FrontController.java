package servlet;

import annotations.AnnotationController;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.RequestDispatcher;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

public class FrontController extends HttpServlet {
    private List<Class<?>> Listecontroller;
    private boolean isChecked;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        StringBuffer url = request.getRequestURL();
        PrintWriter out = response.getWriter();
        out.println("It arrived successfully on :  " + url);
        if (!this.isChecked) {
            String packageScan = this.getInitParameter("package");
            try {
                this.Listecontroller = this.getListeControllers(packageScan);
                this.isChecked = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Class<?> classs : Listecontroller) {
            out.println(classs.getName());
        }
    }

    boolean isController(Class<?> c) {
        return c.isAnnotationPresent(AnnotationController.class);
    }

    List<Class<?>> getListeControllers(String packageName) throws Exception {
        List<Class<?>> res = new ArrayList<Class<?>>();
        String path = this.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        File packageDir = new File(decodedPath);

        File[] files = packageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> classe = Class.forName(className);
                    if (this.isController(classe)) {
                        res.add(classe);
                    }
                }
            }
        }
        return res;

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
}
