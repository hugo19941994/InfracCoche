/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Rafael Mesa Hernández
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

#include <jni.h>
#include <vector>
#include "android/log.h"
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/xfeatures2d.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"

using namespace std;
using namespace cv;
using namespace cv::xfeatures2d;

jclass javaClassRef;
jmethodID javaMethodRef;

// Parameters for 100 km/h
std::vector<KeyPoint> cienKeypoints;
Mat cienDescriptors;


extern "C" {

JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba, jlong addrImg, jlong addrHLS, jlong addrContours);
void SURF_detect(std::vector<cv::Mat> &images, cv::Mat &cienGrey, cv::Mat& mRgb, std::vector<cv::Vec3f>& circles);

JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv* env, jobject, jlong addrGray, jlong addrRgba, jlong addrImg, jlong addrHLS, jlong addrContours)  {
	// Camera frames
	Mat& mGr  = *(Mat*)addrGray; //Grey camera frame
	Mat& mRgb = *(Mat*)addrRgba; //RGB camera frame
    Mat& mHLS = *(Mat*)addrHLS; //RGB camera frame
    Mat& mContours = *(Mat*)addrContours; //RGB camera frame
	//Mat mHLS;
	cvtColor(mRgb, mHLS, COLOR_RGB2HLS); //HLS camera frame

	// 100 km/h image
	Mat& mImg = *(Mat*)addrImg; //100 km/h image
    Mat mImgG;
	cvtColor(mImg, mImgG, COLOR_BGR2GRAY); //Grey 100 km/h image

	// Select red regions
	cv::Mat lower_red_hue_range;
	cv::Mat upper_red_hue_range;
	cv::inRange(mHLS, cv::Scalar(0, 100, 100), cv::Scalar(10, 255, 255), lower_red_hue_range);
	cv::inRange(mHLS, cv::Scalar(160, 100, 100), cv::Scalar(179, 255, 255), upper_red_hue_range);

	// Add blur
	cv::Mat red_hue_image;
	cv::addWeighted(lower_red_hue_range, 1.0, upper_red_hue_range, 1.0, 0.0, red_hue_image);
	cv::GaussianBlur(red_hue_image, red_hue_image, cv::Size(9, 9), 2, 2);

	std::vector<std::vector<cv::Point> > contours;
	cv::findContours( red_hue_image, contours, CV_RETR_LIST, CV_CHAIN_APPROX_NONE );
	for (size_t idx = 0; idx < contours.size(); idx++) {
		cv::drawContours(red_hue_image, contours, idx, Scalar(255,0,0));
	}

	std::vector<cv::Mat> blobs;
	for(int i = 0; i < contours.size(); i++){
		Rect r = boundingRect(contours[i]);
        r.height += 10;
        r.width += 10;
        r.x -= 10;
        r.y -= 10;
		cv::Rect rect_mat(0, 0, mGr.cols, mGr.rows);
		if ((r & rect_mat) == r) //check if its inside
			blobs.push_back(Mat(mGr, r)); // Hough uses Greyscale images
	}

	std::vector<cv::Vec3f> circles2;
	for(cv::Mat m : blobs){
		std::vector<cv::Vec3f> circles1;
		cv::HoughCircles(m, circles1, CV_HOUGH_GRADIENT, 1, 5, 100, 20); // Volver a poner limites
		for(cv::Vec3f f : circles1)
			circles2.push_back(f);
	}

	// Get circles with Hough
	//std::vector<cv::Vec3f> circles;
	//cv::HoughCircles(red_hue_image, circles, CV_HOUGH_GRADIENT, 1, red_hue_image.rows/8, 100, 20, 20, 700);
	std::vector<cv::Mat> images; // Regions of interest

	for(size_t current_circle = 0; current_circle < circles2.size(); ++current_circle) {
		cv::Point center(cvRound(circles2[current_circle][0]), cvRound(circles2[current_circle][1]));
		int radius = cvRound(circles2[current_circle][2]);

		if (center.x - radius >= 0 && center.y - radius >=0 && center.x + radius < mGr.cols && center.y + radius < mGr.rows)
			images.push_back(cv::Mat(mGr, cv::Rect(center.x - radius, center.y - radius, radius * 2, radius * 2)));
	}

    if(images.size() > 0)
        SURF_detect(images, mImgG, mRgb, circles2);

    mContours = red_hue_image;
    //mRgb = red_hue_image;
}

void fill_cien_parameters(cv::Mat &cienGrey) {
	int minHessian = 400;
	const Ptr<SURF> &surf = SURF::create(minHessian);

	surf->detect(cienGrey, cienKeypoints);
	surf->compute(cienGrey, cienKeypoints, cienDescriptors);
}

void SURF_detect(vector<Mat>& images, cv::Mat& cienGrey, cv::Mat& mRgb, std::vector<cv::Vec3f>& circles) {
	if(cienDescriptors.size == 0 || cienKeypoints.size() == 0)
		fill_cien_parameters(cienGrey);

	for(unsigned int i = 0; i < images.size(); ++i) {
        //-- Step 1: Extract frame keypoints
		int minHessian = 400;
		const Ptr<SURF> &surf = SURF::create(minHessian);
		std::vector<KeyPoint> frameKeypoints;
		surf->detect(images.at(i), frameKeypoints);

		//-- Step 2: Calculate descriptors (feature vectors)
		Mat frameDescriptors;
		surf->compute(images.at(i), frameKeypoints, frameDescriptors);

		//-- Step 3: Matching descriptor vectors using FLANN matcher
		FlannBasedMatcher matcher;
		std::vector< DMatch > matches;
        if(frameDescriptors.empty() || frameKeypoints.empty())
            continue; // Skip iteration if there are no descriptors/keypoints
        matcher.match(cienDescriptors, frameDescriptors, matches);
		double max_dist = 0; double min_dist = 100;

		//-- Quick calculation of max and min distances between keypoints
		for (int j = 0; j < frameDescriptors.rows; j++) {
			double dist = matches[j].distance;
			if( dist < min_dist ) min_dist = dist;
			if( dist > max_dist ) max_dist = dist;
		}

		printf("-- Max dist : %f \n", max_dist );
		printf("-- Min dist : %f \n", min_dist );

		//-- Draw only "good" matches (i.e. whose distance is less than 2*min_dist,
		//-- or a small arbitary value ( 0.02 ) in the event that min_dist is very
		//-- small)
		//-- PS.- radiusMatch can also be used here.

		std::vector< DMatch > good_matches;
		for (int j = 0; j < frameDescriptors.rows; j++) {
            if (matches[j].distance <= max(2*min_dist, 0.02))
			    good_matches.push_back( matches[j]);
		}

		for (int j = 0; j < (int)good_matches.size(); j++)
            __android_log_print( ANDROID_LOG_DEBUG, "LOG_TAG","-- Good Match [%d] Keypoint 1: %d  -- Keypoint 2: %d  \n", j, good_matches[j].queryIdx, good_matches[j].trainIdx );

        if (good_matches.size() > 10) { // Try different values
            cv::Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));
            int radius = cvRound(circles[i][2]);

            circle(mRgb, Point(center.x, center.y), radius, Scalar(0, 255, 0, 255), 3);
        }
	}
}

}
