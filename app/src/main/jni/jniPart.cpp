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

    // Read stored sample and label for training
    if (sample.rows == 0 && response.rows == 0) {
        fillLabel();
        fillTraining();
        trainingData = TrainData::create(sample, SampleTypes::ROW_SAMPLE, response);
        knn->setIsClassifier(true);
        knn->setAlgorithmType(KNearest::Types::BRUTE_FORCE);
        knn->setDefaultK(1);

        knn->train(trainingData); // Train with sample and responses
    }

    // Return an array with traffis signals
    jintArray result;
    result = (env)->NewIntArray(5);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    int i;
    // fill a temp structure to use to populate the java int array
    jint fill[256];
    int numSe = 0;
    for(int i = 0; i < 5; i++){
        fill[i] = 1;
    }

    Mat &src = *(Mat *) frame; //RGB camera frame
    Mat originalSrc;
    src.copyTo(originalSrc);

    cv::Mat lab_image;
    cv::cvtColor(src, lab_image, CV_RGB2Lab); // Usar formato RGB, no BGR!!
    std::vector<cv::Mat> lab_planes(3);
    cv::split(lab_image, lab_planes);
    cv::Ptr<cv::CLAHE> clahe = cv::createCLAHE();
    clahe->setClipLimit(4);
    cv::Mat dst2;
    clahe->apply(lab_planes[0], dst2);
    dst2.copyTo(lab_planes[0]);
    cv::merge(lab_planes, lab_image);
    cv::cvtColor(lab_image, src, CV_Lab2RGB);

    //erode(src, src, Mat());
    //dilate(src, src, Mat());

    //Process image to extract contour
    Mat mHSV;
    cvtColor(src, mHSV, COLOR_RGB2HSV); //HLS camera frame

    // Select red regions
    cv::Mat mRedHue;
    cv::Mat lower_red_hue_range;
    cv::Mat upper_red_hue_range;
    cv::inRange(mHSV, cv::Scalar(0, 100, 100), cv::Scalar(10, 255, 255), lower_red_hue_range);
    cv::inRange(mHSV, cv::Scalar(160, 100, 100), cv::Scalar(179, 255, 255), upper_red_hue_range);

    // Add blur
    cv::addWeighted(lower_red_hue_range, 1.0, upper_red_hue_range, 1.0, 0.0, mRedHue);
    cv::GaussianBlur(mRedHue, mRedHue, cv::Size(9, 9), 2, 2);

    // Extract Contours from binary image
    cv::Mat contourMat;
    Mat response_array;
    std::vector<std::vector<cv::Point> > contours;
    vector< Vec4i > hierarchy;
    Canny(mRedHue, mRedHue, 10, 25, 3);
    cv::findContours(mRedHue, contours, hierarchy, CV_RETR_EXTERNAL, CHAIN_APPROX_SIMPLE, Point(0, 0));

    Mat dst(src.rows, src.cols, CV_8UC3, Scalar::all(0));

    for (int i = 0; i < contours.size(); i = hierarchy[i][0]) // iterate through each contour for first hierarchy level .
    {
        //approxPolyDP(Mat(contours[i]), contours[i], 5, true);
        drawContours(src, contours, -1, Scalar(0, 255, 0));
        Rect r = boundingRect(contours[i]);

        Mat ROI = src(r);
        if ((ROI.cols > 2* ROI.rows) || (ROI.rows > ROI.cols*2 ) || ROI.cols < 20 || ROI.rows < 20)
            continue;
        Mat ROICanny = mRedHue(r);

        std::vector<cv::Vec3f> circles;
        Mat ROIg;

        cvtColor(ROI, ROIg, CV_RGB2GRAY); //HLS camera frame

        //threshold(ROIg, ROIg, 200, 255, 0);
        cv::HoughCircles(ROICanny, circles, CV_HOUGH_GRADIENT, 2, ROIg.rows/4, 200, 100);
        for (Vec3f c : circles) {
            circle(ROI, Point(c[0], c[1]), c[2], Scalar(0, 255, 0));
        }
        if (circles.size() != 0) {
            Mat tmp1, tmp2, tmp3;
            resize(ROIg, tmp1, Size(50, 50), 0, 0, INTER_LINEAR);
            tmp1.convertTo(tmp2, CV_32FC1);
            int found = knn->findNearest(tmp2.reshape(1, 1), knn->getDefaultK(), tmp3);
            std::stringstream buffer;
            buffer << tmp3;
            putText(src, buffer.str(), Point(r.x, r.y + r.height), 0, 1, Scalar(0, 255, 0), 2, 8);
            rectangle(src, r, Scalar(0, 0, 255));
            fill[numSe] = found;
            numSe++;
        }
    }

    // Switch viewing mode
    switch(mode) {
        case(0):
            break;
        case(1):
            src = originalSrc;
            break;
        case(2):
            src = originalSrc;
            break;
        case(3):
            src = mRedHue;
            break;
    }

    // move from the temp structure to the java structure
    (env)->SetIntArrayRegion(result, 0, 5, fill);
    return result;
}
}
