#include "jni_visual.h"
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>
#include <iostream>
#include <android/log.h>

using namespace std;
using namespace cv;

extern "C" {

CvMemStorage * g_storage = NULL;
Rect selection;	//用于保存鼠标选择的矩形框
Rect trackWindow;
RotatedRect trackBox;//定义一个旋转的矩阵类对象

int trackObject = 0;
float hranges[] = { 0, 180 };	//hranges在后面的计算直方图函数中要用到
const float* phranges = hranges;
int hsize = 16;
int vmin = 10, vmax = 256, smin = 30;
float position[] = { 0, 0, 0, 0 };
Mat hue, mask, hist, histimg = Mat::zeros(200, 320, CV_8UC3), backproj;

JNIEXPORT void JNICALL Java_ct_codec_dji_openCV_NativeController_TrackingArea(JNIEnv*, jobject, jint startX, jint startY, jint width, jint height) {
	selection = Rect(startX, startY, 0, 0);
	selection.x = startX;
	selection.y = startY;
	selection.width = width;
	selection.height = height;
	trackObject = -1;

	LOGD("selection x: %d", selection.x);
	LOGD("selection y: %d", selection.y);
	LOGD("selection width: %d", selection.width);
	LOGD("selection height: %d", selection.height);
}

JNIEXPORT jfloatArray JNICALL Java_ct_codec_dji_openCV_NativeController_Track(JNIEnv *env, jobject, jlong addrSource, jlong addrGray, jlong addrCanny) {
	Mat * pMatIn =(Mat * ) addrSource;
	Mat * pMatGray = (Mat * ) addrGray;
	Mat * pMatOut = (Mat * ) addrCanny;

//	cvtColor(*pMatIn, *pMatGray, CV_BGR2GRAY);
//	CvSeq * contours = 0;
//
//	if (g_storage == NULL) {
//		g_storage = cvCreateMemStorage(0);
//	} else {
//		cvClearMemStorage(g_storage);
//	}
//	cvCvtColor(g_image, g_gray, CV_BGR2GRAY);
//	IplImage pGray= IplImage(*pMatGray);
//	IplImage pOut = IplImage(*pMatOut);
//
//	cvThreshold(&pGray, &pOut, g_thresh, 255, CV_THRESH_BINARY);
//	cvFindContours(&pOut, g_storage, &contours);
//	cvZero(&pOut);
//	if (contours) {
//		cvDrawContours(&pOut, contours, cvScalarAll(255), cvScalarAll(255), 100);
//	}

//	cvThreshold(*g_gray, *g_gray, g_thresh, 255, CV_THRESH_BINARY);
//	cvFindContours(*g_gray, g_storage, &contours);
//	Canny (*pMatGray, *pMatOut, 50, 150, 3) ;


	cvtColor(*pMatIn, *pMatGray, CV_BGR2HSV);
	int ch[] = { 0, 0 };

	Mat hsv = *pMatGray;
	hue.create(hsv.size(), hsv.depth());
	mixChannels(&hsv, 1, &hue, 1, ch, 1);	//将xv第一个通道(也就是色调)的数复制到hue中，0索引数组

	inRange(hsv, Scalar(0, smin, MIN(vmin, vmax)), Scalar(180, 256, MAX(vmin, vmax)), mask);

//	LOGD("hue col: %d", hue.cols);
//	LOGD("hue rows: %d", hue.rows);

	if (trackObject < 0) {
		Mat roi(hue, selection), maskroi(mask, selection); //mask保存的hsv的最小值

		//calcHist()函数第一个参数为输入矩阵序列，第2个参数表示输入的矩阵数目，第3个参数表示将被计算直方图维数通道的列表，第4个参数表示可选的掩码函数
		//第5个参数表示输出直方图，第6个参数表示直方图的维数，第7个参数为每一维直方图数组的大小，第8个参数为每一维直方图bin的边界
		calcHist(&roi, 1, 0, maskroi, hist, 1, &hsize, &phranges);	//将roi的0通道计算直方图并通过mask放入hist中，hsize为每一维直方图的大小
		normalize(hist, hist, 0, 255, CV_MINMAX);	//将hist矩阵进行数组范围归一化，都归一化到0~255

		trackWindow = selection;
		trackObject = 1;

		histimg = Scalar::all(0);
		int binW = histimg.cols / hsize;  //histing是一个200*300的矩阵，hsize应该是每一个bin的宽度，也就是histing矩阵能分出几个bin出来
		Mat buf(1, hsize, CV_8UC3);	//定义一个缓冲单bin矩阵
		for (int i = 0; i < hsize; i++) {	//saturate_case函数为从一个初始类型准确变换到另一个初始类型
			buf.at<Vec3b>(i) = Vec3b(saturate_cast<uchar>(i*180. / hsize), 255, 255);	//Vec3b为3个char值的向量
		}
		cvtColor(buf, buf, CV_HSV2BGR);//将hsv又转换成bgr

		for (int i = 0; i < hsize; i++) {
			int val = saturate_cast<int>(hist.at<float>(i)*histimg.rows / 255);//at函数为返回一个指定数组元素的参考值
			rectangle(histimg, Point(i*binW, histimg.rows),    //在一幅输入图像上画一个简单抽的矩形，指定左上角和右下角，并定义颜色，大小，线型等
					Point((i + 1)*binW, histimg.rows - val),
					Scalar(buf.at<Vec3b>(i)), -1, 8);
		}
	}


	calcBackProject(&hue, 1, 0, hist, backproj, &phranges);//计算直方图的反向投影，计算hue图像0通道直方图hist的反向投影，并让入backproj中
	backproj &= mask;

	RotatedRect trackBox = CamShift(backproj, trackWindow,      //trackWindow为鼠标选择的区域，TermCriteria为确定迭代终止的准则
	TermCriteria(CV_TERMCRIT_EPS | CV_TERMCRIT_ITER, 10, 1));	//CV_TERMCRIT_EPS是通过forest_accuracy,CV_TERMCRIT_ITER
	if (trackWindow.area() <= 1) {	//是通过max_num_of_trees_in_the_forest
		int cols = backproj.cols, rows = backproj.rows, r = (MIN(cols, rows) + 5) / 6;
		trackWindow = Rect(trackWindow.x - r, trackWindow.y - r,
				trackWindow.x + r, trackWindow.y + r) &
				Rect(0, 0, cols, rows);	//Rect函数为矩阵的偏移和大小，即第一二个参数为矩阵的左上角点坐标，第三四个参数为矩阵的宽和高
	}

//	if (backprojMode)
//	cvtColor(backproj, image, CV_GRAY2BGR);//因此投影模式下显示的也是rgb图？
	rectangle(*pMatIn, trackBox.boundingRect(), Scalar(0, 135, 136), 3, CV_AA);

	position[0] = trackBox.center.x;
	position[1] = trackBox.center.y;
	position[2] = trackBox.boundingRect().width;
	position[3] = trackBox.boundingRect().height;

	jfloatArray array = env->NewFloatArray(4);
	env->SetFloatArrayRegion(array, 0, 4, position);
	return array;
}



}
