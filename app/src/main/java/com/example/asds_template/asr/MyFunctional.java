package com.example.asds_template.asr;

import java.util.Arrays;

/**
 * Created by harry on 11/15/15.
 */
public class MyFunctional {
    public static float[] mysort(float[] mat, int rnum, int cnum, int idx){
        float[] sorted = new float[cnum];
        for(int i = 0;i<cnum;i++)
            sorted[i] = mat[rnum*i+idx];

        Arrays.sort(sorted);
        return sorted;
    }

    public static float quartile1(float[] sortedArr){
        int size = sortedArr.length/4;
        float q1=0;
        for(int i=0;i<size;i++)
            q1+=sortedArr[i];
        q1/=size;
        return q1;
    }

    public static float quartile3(float[] sortedArr){
        int size = sortedArr.length/4;
        int len = sortedArr.length;
        float q3=0;
        for(int i=0;i<size;i++)
            q3+=sortedArr[len-i-1];
        q3/=size;
        return q3;
    }

    public static float percentile1(float[] sortedArr){
        int size = 1+sortedArr.length/100;
        float per1=0;
        for(int i=0;i<size;i++)
            per1+=sortedArr[i];
        per1/=size;
        return per1;
    }

    public static float Stddev(float[] mat, int rnum, int cnum, int idx){
        float mean = Mean(mat,rnum,cnum,idx),var = 0;
        for(int i = 0;i<cnum;i++)
            var+=mat[rnum*i+idx]*mat[rnum*i+idx];
        return (float) Math.sqrt((double)(var/cnum-mean*mean));
    }

    public static float Mean(float[] mat, int rnum, int cnum, int idx){
        float mean=0;
        for(int i = 0;i<cnum;i++)
            mean+=mat[rnum*i+idx];
        mean/=cnum;
        return mean;
    }

    public static float LinearRegErrorA(float[] mat, int rnum, int cnum, int idx){
        float sumX = 0, sumXX=0, sumY=0, sumXY=0;
        float A,b,err=0;
        for(int i = 0;i<cnum;i++){
            sumX+=i;
            sumXX+=i*i;
            sumY+=mat[rnum*i+idx];
            sumXY+=i*mat[rnum*i+idx];
        }
        A = (sumXY-sumX*sumY/cnum)/(sumXX-sumX*sumX/cnum);
        b = sumY/cnum-A*sumX/cnum;
        for(int i=0;i<cnum;i++){
            err+=Math.abs(A*i+b-mat[rnum*i+idx]);
        }
        return err/cnum;
    }

    public static float LinearCoefA(float[] mat, int rnum, int cnum, int idx){
        float sumX = 0, sumXX=0, sumY=0, sumXY=0;
        float A;
        for(int i = 0;i<cnum;i++){
            sumX+=i;
            sumXX+=i*i;
            sumY+=mat[rnum*i+idx];
            sumXY+=i*mat[rnum*i+idx];
        }
        A = (sumXY-sumX*sumY/cnum)/(sumXX-sumX*sumX/cnum);
        return A;
    }

    public static float dot(float[] a,float[] b){
        float value = 0;
        for(int i=0;i<a.length;i++)
            value+=a[i]*b[i];
        return value;
    }
}
