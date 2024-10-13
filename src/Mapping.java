package utils;

import java.lang.reflect.Method;
import utils.VerbAction;
import java.util.*;

public class Mapping {
    String className;
    List<VerbAction> VerbAction;

    public Mapping() {
        
    }

    public Mapping(String className) {
        this.className = className;
        this.VerbAction = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<VerbAction> getVerbAction() {
        return VerbAction;
    }

    public void addVerbAction(String method, String verb) {
        this.VerbAction.add(new VerbAction(method, verb));
    }

}
