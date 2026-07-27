package com.example.dhparameterfx;

public record DHParameter(double a, double alpha, double d, double theta) {

    public Matrix4x4 toTransformMatrix(){

        // parameters
        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);
        double cosA = Math.cos(alpha);
        double sinA = Math.sin(alpha);

        return new Matrix4x4(new double[][]{
            { cosT, -sinT * cosA, sinT * sinA, a * cosT},
            { sinT, cosT * cosA, -cosT * sinA, a * sinT},
            {0, sinA, cosA, d, },
            {0, 0, 0, 1}
        });
    }
}
