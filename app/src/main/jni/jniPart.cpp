#include <jni.h>
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/xfeatures2d.hpp"
#include <vector>
#include <opencv2/objdetect.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui.hpp>
#include <android/log.h>

using namespace std;
using namespace cv;
using namespace cv::xfeatures2d;

String face_cascade_name = "lbpcascade_frontalface.xml";
String eyes_cascade_name = "haarcascade_eye_tree_eyeglasses.xml";
CascadeClassifier face_cascade;
CascadeClassifier eyes_cascade;
String window_name = "Capture - Face detection";

extern "C" {
JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba, jlong addrImg);
JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFace(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba, jlong addrImg)
{
	//https://es.wikipedia.org/wiki/Anexo:Se%C3%B1ales_de_limitaci%C3%B3n_de_velocidad_de_Espa%C3%B1a#

	Mat& mGr  = *(Mat*)addrGray;
	Mat& mRgb = *(Mat*)addrRgba;
	Mat& mImg = *(Mat*)addrImg;
    Mat mImgG;
    cvtColor(mImg, mImgG, COLOR_BGR2GRAY);
	vector<KeyPoint> v;

	//Ptr<FeatureDetector> detector = FastFeatureDetector::create(50);
	//detector->detect(mGr, v);

	//ONLY FOR TESTING
	//-- Step 1: Detect the keypoints using SURF Detector
    if (!mImgG.data)
        exit(0);
	int minHessian = 400;
	const Ptr<SURF> &surf = SURF::create(minHessian);
    std::vector<KeyPoint> keypoints1;
    std::vector<KeyPoint> keypoints2;
    //surf->detect(mGr, keypoints1);
    surf->detect(mGr, keypoints1);
    surf->detect(mImgG, keypoints2);

	//-- Step 2: Calculate descriptors (feature vectors)
	//  SurfDescriptorExtractor extractor;
	Mat descriptors1, descriptors2;
    surf->compute(mGr, keypoints1, descriptors1);
    surf->compute(mImgG, keypoints2, descriptors2);

	//-- Step 3: Matching descriptor vectors using FLANN matcher
	FlannBasedMatcher matcher;
	std::vector< DMatch > matches;
	matcher.match(descriptors1, descriptors2, matches);
	double max_dist = 0; double min_dist = 100;

	//-- Quick calculation of max and min distances between keypoints
	for( int i = 0; i < descriptors1.rows; i++ )
	{ double dist = matches[i].distance;
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
	for( int i = 0; i < descriptors1.rows; i++ )
	{ if( matches[i].distance <= max(2*min_dist, 0.02) )
		{ good_matches.push_back( matches[i]); }
	}

	//-- Draw only "good" matches
	Mat img_matches;
	drawMatches( mGr, keypoints1, mImgG, keypoints2,
				 good_matches, img_matches, Scalar::all(-1), Scalar::all(-1),
				 vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS );

	//-- Show detected matches
	//imshow( "Good Matches", img_matches );

	for( int i = 0; i < (int)good_matches.size(); i++ )
	{ __android_log_print( ANDROID_LOG_DEBUG, "LOG_TAG","-- Good Match [%d] Keypoint 1: %d  -- Keypoint 2: %d  \n", i, good_matches[i].queryIdx, good_matches[i].trainIdx ); }

    for( unsigned int i = 0; i < good_matches.size(); i++ )
    {
        KeyPoint &kp = keypoints1[good_matches[i].queryIdx];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }

	//waitKey(0);
}

JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFace(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;

    circle(mRgb, Point(500, 500), 10, Scalar(255,0,0,255));

    /*std::vector<Rect> faces;
	Mat frame_gray;

	cvtColor( mRgb, mGr, COLOR_BGR2GRAY );
	equalizeHist( frame_gray, frame_gray );

	-- Detect faces
	face_cascade.detectMultiScale( mGr, faces, 1.1, 2, 0, Size(80, 80) );

	for( size_t i = 0; i < faces.size(); i++ )
	{
		Mat faceROI = frame_gray( faces[i] );
		std::vector<Rect> eyes;

		//-- In each face, detect eyes
		eyes_cascade.detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(30, 30) );
		if( eyes.size() == 2)
		{
			//-- Draw the face
			Point center( faces[i].x + faces[i].width/2, faces[i].y + faces[i].height/2 );
			ellipse( mRgb, center, Size( faces[i].width/2, faces[i].height/2 ), 0, 0, 360, Scalar( 255, 0, 0 ), 2, 8, 0 );

			for( size_t j = 0; j < eyes.size(); j++ )
			{ //-- Draw the eyes
				Point eye_center( faces[i].x + eyes[j].x + eyes[j].width/2, faces[i].y + eyes[j].y + eyes[j].height/2 );
				int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );
				circle( mRgb, eye_center, radius, Scalar( 255, 0, 255 ), 3, 8, 0 );
			}
		}

        }
        //-- Show what you got
        //imshow( window_name, frame );
         */
}
}
