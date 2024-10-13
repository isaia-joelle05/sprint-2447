package utils;

public class VerbAction {
    String methodName;
    String verb;

    public VerbAction(String methodName, String verb) {
        this.methodName = methodName;
        this.verb = verb;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }
}