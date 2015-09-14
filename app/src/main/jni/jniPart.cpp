#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <vector>

using namespace std;
using namespace cv;

String face_cascade_name = "lbpcascade_frontalface.xml";
String eyes_cascade_name = "haarcascade_eye_tree_eyeglasses.xml";
CascadeClassifier face_cascade;
CascadeClassifier eyes_cascade;
String window_name = "Capture - Face detection";

extern "C" {
JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);
JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFace(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL Java_com_example_uemcar_Camera_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
	//https://es.wikipedia.org/wiki/Anexo:Se%C3%B1ales_de_limitaci%C3%B3n_de_velocidad_de_Espa%C3%B1a#

	Mat& mGr  = *(Mat*)addrGray;
	Mat& mRgb = *(Mat*)addrRgba;
	vector<KeyPoint> v;

	Ptr<FeatureDetector> detector = FastFeatureDetector::create(50);
	detector->detect(mGr, v);
    for( unsigned int i = 0; i < v.size(); i++ )
    {
		const KeyPoint& kp = v[i];
		circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }

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
