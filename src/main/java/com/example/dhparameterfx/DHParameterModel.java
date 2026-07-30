package com.example.dhparameterfx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class DHParameterModel {
    private final DoubleProperty a = new SimpleDoubleProperty();
    private final DoubleProperty alpha = new SimpleDoubleProperty();
    private final DoubleProperty d = new SimpleDoubleProperty();
    private final DoubleProperty theta = new SimpleDoubleProperty();

    public DHParameterModel(double a, double alphaDeg, double d, double thetaDeg) {
        setA(a);
        setAlpha(alphaDeg);
        setD(d);
        setTheta(thetaDeg);
    }

    public DHParameter toDHParameter() {
        return new DHParameter(getA(), Math.toRadians(getAlpha()), getD(), Math.toRadians(getTheta()));
    }

    // --- Property Getters & Setters ---
    public double getA() { return a.get(); }
    public void setA(double value) { a.set(value); }
    public DoubleProperty aProperty() { return a; }

    public double getAlpha() { return alpha.get(); }
    public void setAlpha(double value) { alpha.set(value); }
    public DoubleProperty alphaProperty() { return alpha; }

    public double getD() { return d.get(); }
    public void setD(double value) { d.set(value); }
    public DoubleProperty dProperty() { return d; }

    public double getTheta() { return theta.get(); }
    public void setTheta(double value) { theta.set(value); }
    public DoubleProperty thetaProperty() { return theta; }
}