/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

#include <jni.h>
#include <vector>
#include <iostream>
#include <sstream>
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/xfeatures2d.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/text/ocr.hpp"
#include "opencv2/opencv.hpp"
#include <stdio.h>

using namespace std;
using namespace cv;
using namespace cv::xfeatures2d;
using namespace cv::ml;

Mat response, sample;
Ptr<TrainData> trainingData;
Ptr<KNearest> knn = KNearest::create();
int trained = 0;

template <typename T>
std::string to_string(T value)
{
    //create an output string stream
    std::ostringstream os ;

    //throw the value into the string stream
    os << value ;

    //convert the string stream into a string and return
    return os.str() ;
}

void fillTraining() {
    FileStorage Data("/sdcard/trainingdata.yml", FileStorage::READ); // Read traing data to a Mat
    Data["data"] >> sample;
    Data.release();

}

void fillLabel() {
    FileStorage Label("/sdcard/labeldata.yml", FileStorage::READ); // Read label data to a Mat
    Label["label"] >> response;
    Label.release();
}

extern "C" {

JNIEXPORT jintArray JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv *env, jclass cls,
                                                                   jlong frame, jint mode) {

    // Leer los datos de entrenamiento una vez
    // Poner en su propia función y llamarla desde java
    if (trained == 0) {
        fillLabel();
        fillTraining();
        trainingData = TrainData::create(sample, SampleTypes::ROW_SAMPLE, response);
        knn->setIsClassifier(true);
        knn->setAlgorithmType(KNearest::Types::BRUTE_FORCE);
        knn->setDefaultK(1);
        knn->train(trainingData); // Train with sample and responses
        trained = 1;
    }

    // Preparación para devolver un array de 5 ints
    // https://stackoverflow.com/questions/1610045/how-to-return-an-array-from-jni-to-java
    jintArray result;
    result = (env)->NewIntArray(5);
    if (result == NULL)
        return NULL; /* out of memory error thrown */
    jint fill[256];
    int numSe = 0;
    for(int i = 0; i < 5; i++)
        fill[i] = 1;

    // Frame de la camara pasada desde el JNI
    Mat &src = *(Mat *) frame;  // Formato RGB!! (por defecto OpenCV usa BGR)
    Mat originalSrc;
    src.copyTo(originalSrc);

    // CLAHE demasiado lento - ajuste dinamico??

    // Erode & Dilate quita muchos puntos innecesarios
    erode(src, src, Mat());
    dilate(src, src, Mat());

    // Pasar a HSV
    Mat mHSV, mHSV2;
    cvtColor(src, mHSV, COLOR_RGB2HSV);
    mHSV.copyTo(mHSV2);

    // Regiones rojas
    cv::Mat mRedHue;
    cv::Mat lower_red_hue_range;
    cv::Mat upper_red_hue_range;
    cv::inRange(mHSV, cv::Scalar(0, 100, 100), cv::Scalar(10, 255, 255), lower_red_hue_range);
    cv::inRange(mHSV, cv::Scalar(160, 100, 100), cv::Scalar(179, 255, 255), upper_red_hue_range);

    // Gaussian blur
    cv::addWeighted(lower_red_hue_range, 1.0, upper_red_hue_range, 1.0, 0.0, mRedHue);
    cv::GaussianBlur(mRedHue, mRedHue, cv::Size(9, 9), 2, 2);
    Mat mRedHue2;
    mRedHue.copyTo(mRedHue2);

    // Extraer contornos (blobs) usando Canny y findContours
    cv::Mat contourMat;
    Mat response_array;
    std::vector<std::vector<cv::Point> > contours;
    vector< Vec4i > hierarchy;
    Canny(mRedHue, mRedHue, 10, 25, 3);
    cv::findContours(mRedHue, contours, hierarchy,
                     CV_RETR_EXTERNAL, CHAIN_APPROX_SIMPLE, Point(0, 0));

    // Recorrer todos los contornos con nivel jerárquico 0
    for (int i = 0; i < contours.size(); i = hierarchy[i][0]) {
        //approxPolyDP para el STOP?

        Rect r = boundingRect(contours[i]);
        Mat ROI = src(r);

        // Comprobar tamaño y ratio - si es demasiado pequeño salir de iteración
        if ((ROI.cols > 2* ROI.rows) || (ROI.rows > ROI.cols*2 ) || ROI.cols < 20 || ROI.rows < 20)
            continue;

        // Dibujar circulos en Mat
        drawContours(src, contours, -1, Scalar(0, 255, 0));
        Mat ROICanny = mRedHue(r);

        std::vector<cv::Vec3f> circles;
        Mat ROIg;

        // Detectar circulos
        cvtColor(ROI, ROIg, CV_RGB2GRAY); // HoughCircles acepta solo imagenes en escala de grises
        cv::HoughCircles(ROIg, circles, CV_HOUGH_GRADIENT, 1, ROIg.rows, 120, 60); // Añadir max y min

        // Dibujar circulos en Mat
        for (Vec3f c : circles)
            circle(ROI, Point(c[0], c[1]), c[2], Scalar(0, 255, 0));

        // Analizar blob si hemos encontrado por lo menos un circulo
        if (circles.size() != 0) {
            Mat tmp;
            resize(ROIg, tmp, Size(50, 50), 0, 0, INTER_LINEAR);  // Normalizar a 50x50
            tmp.convertTo(tmp, CV_32FC1);  // Convertir a coma flotante

            // Comprarar usando KNN
            int found = knn->findNearest(tmp.reshape(1, 1), knn->getDefaultK(), tmp);

            putText(src, to_string(found), Point(r.x, r.y + r.height), 0, 1,
                    Scalar(0, 255, 0), 2, 8);  // Poner texto en imagen
            rectangle(src, r, Scalar(0, 0, 255));  // Dibujar rectangulo del ROI

            fill[numSe] = found;  // Añadir resultado al array de señales encontradas
            numSe++;
        }
    }

    // Modo de visión seleccionado
    switch(mode) {
        case(0):  // Result
            break;
        case(1):  // RGB
            src = originalSrc;
            break;
        case(2):  // Gray
            cvtColor(originalSrc, src, CV_RGB2GRAY);
            break;
        case(3):  // HSV
            src = mHSV2;
            break;
        case(4):  // Red Hue
            src = mRedHue2;
            break;
        case(5):  // Contours
            src = mRedHue;
            break;
    }

    // Devolver array con señales encontradas
    (env)->SetIntArrayRegion(result, 0, 5, fill);
    return result;
}
}
