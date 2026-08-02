package com.example.dhparameterfx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class DHParameterModel {
    private final DoubleProperty a = new SimpleDoubleProperty();
    private final DoubleProperty alpha = new SimpleDoubleProperty();
    private final DoubleProperty d = new SimpleDoubleProperty();
    private final DoubleProperty theta = new SimpleDoubleProperty();

    // Joint Limits (Defaults: -135° to 135° to avoid self-collision overlaps)
    private final DoubleProperty minTheta = new SimpleDoubleProperty(-135.0);
    private final DoubleProperty maxTheta = new SimpleDoubleProperty(135.0);

    public DHParameterModel(double a, double alpha, double d, double theta) {
        this.a.set(a);
        this.alpha.set(alpha);
        this.d.set(d);
        this.theta.set(theta);
    }

    public DHParameterModel(double a, double alpha, double d, double theta, double minTheta, double maxTheta) {
        this.a.set(a);
        this.alpha.set(alpha);
        this.d.set(d);
        this.theta.set(theta);
        this.minTheta.set(minTheta);
        this.maxTheta.set(maxTheta);
    }

    // Standard Getters & Properties
    public double getA() { return a.get(); }
    public DoubleProperty aProperty() { return a; }

    public double getAlpha() { return alpha.get(); }
    public DoubleProperty alphaProperty() { return alpha; }

    public double getD() { return d.get(); }
    public DoubleProperty dProperty() { return d; }

    public double getTheta() { return theta.get(); }
    public void setTheta(double val) { this.theta.set(val); }
    public DoubleProperty thetaProperty() { return theta; }

    public double getMinTheta() { return minTheta.get(); }
    public DoubleProperty minThetaProperty() { return minTheta; }

    public double getMaxTheta() { return maxTheta.get(); }
    public DoubleProperty maxThetaProperty() { return maxTheta; }

    public DHParameter toDHParameter() {
        return new DHParameter(getA(), getAlpha(), getD(), getTheta());
    }
}