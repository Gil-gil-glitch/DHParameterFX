package com.example.dhparameterfx;

public class Matrix4x4 {


    private final double[][] m = new double[4][4];

    public Matrix4x4(double[][] doubles) {

        for (int i = 0; i < 4; i++){

            System.arraycopy(doubles[i], 0, m[i], 0, 4);

        }
    }

    public static Matrix4x4 identiy() {

        return new Matrix4x4(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });

    }

    public Matrix4x4 multiply(Matrix4x4 second){

        double[][] result = new double[4][4];

        for (int row = 0; row < 4; row++){

            for (int col = 0; col < 4; col++){

                for (int k = 0; k < 4; k++){
                    result[row][col] += this.m[row][k] * second.m[k][col];
                }
            }
        }
        return new Matrix4x4(result);
    }

    public double get(int row, int col){

        return m[row][col];

    }

    public double[] getPosition(){

        return new double[]{
                m[0][3], m[1][3], m[2][3]
        };
    }

    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 4; i++){

            sb.append(String.format("[ %8.3f %8.3f %8.3f %8.3f ]\n", m[i][0], m[i][1], m[i][2], m[i][3]));
        }

        return sb.toString();
    }
}
