package changerings;

//Visualization_02

//ORIGINAL CODE FROM: http://bgo.la/soundandvision.pde
//CHANGE RINGS FROM: http://www.changeringing.co.uk/handbells.htm  --THERE ARE MORE!

//COMMENTS IN CAPS ARE MINE

import java.awt.AWTException;
import java.awt.Robot;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PVector;

import SimpleOpenNI.SimpleOpenNI;

import ddf.minim.AudioInput;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.FFT;

// Sounds processing

public class Visualization_02_mouse_follow extends PApplet {
	boolean SHOW_MOUSE_COORDINATES = false;
	boolean SHOW_DEPTH_FIELD = false;
	boolean SHOW_SKELETONS = true;
	
	//Start music when person comes into view, otherwise on mouse click
	boolean START_MUSIC_ON_PERSON = true;

	Minim minim;
	AudioPlayer song;
	FFT fft;
	AudioInput in;

	int c1;
	int RASTRO = 10;
	float[] bands;
	int v;

	// Kinect and variables
	SimpleOpenNI kinect;
	boolean usersSensed = false;
	
	float zoomF = 0.85f; // how big the skeleton appears in the frame
	float rotX = radians(180); // by default rotate the hole scene 180deg around
								// the x-axis,
								// the data from openni comes upside down
	float rotY = radians(0);
	boolean autoCalib = true;

	PVector bodyCenter = new PVector();
	PVector bodyDir = new PVector();
	PVector com = new PVector();
	PVector com2d = new PVector();
	int[] userClr = new int[] { color(255, 0, 0), color(0, 255, 0), color(0, 0, 255), color(255, 255, 0),
			color(255, 0, 255), color(0, 255, 255) };

	// Robot class used to move mouse around
	Robot robot = null;

	public boolean sketchFullScreen() {
		return true;
	}

	// ...............................................................
	// SETUP..............

	public void setup() {
		// strokeWeight(0);
		size(displayWidth, displayHeight, P3D);

		try {
			robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean kinectAttached = setupKinect(this);

		minim = new Minim(this);

		loadAudioFile();

		// colorMode(HSB); THIS COLOR MODE DRIVES ME CRAZY -- I SHOULD LEARN
		// THIS....
		ellipseMode(CENTER);
		rectMode(CORNER);

		// get a line in from Minim, default bit depth is 16
		in = minim.getLineIn(Minim.STEREO, 512);
		v = 0;
		// fft = new FFT(in.bufferSize(), in.sampleRate());
		fft = new FFT(song.bufferSize(), song.sampleRate());
		fft.window(FFT.HAMMING);

		// initialize peak-hold structures - I TOOK THIS OUT BECAUSE I COULDN'T
		// FIGURE OUT WHAT IT DID
		/*
		 * peaksize = 1+Math.round(fft.specSize()/binsperband); peaks = new
		 * float[peaksize]; peak_age = new int[peaksize];
		 */
		frameRate(30);

		smooth();
		perspective(radians(45), width / height, 10, 150000);
	}

	private void loadAudioFile() {
		// song = minim.loadFile("sabre_dance.mp3", 2048); // VARIOUS MUSIC
		// SELECTIONS
		// song = minim.loadFile("sabre_dance_slow.mp3", 2048);
		song = minim.loadFile("5040gr15b.mp3", 2048); // MY OTHER FAVORITE
		// song = minim.loadFile("5184lo08a.mp3", 2048);

		// song = minim.loadFile("12345st11c.mp3", 2048); // NEW FAVORITE
		// song = minim.loadFile("5080lo10a.mp3", 2048); // MY FAVORITE
		// song = minim.loadFile("5080lo10a_slow.mp3", 2048);
	}
	
	// //////////////////////////////////////////////////////DRAW LOOP
	// /////////////////// // END DRAW LOOP

	public void draw() {
		background(0, 0, 0);
		
		// MUSIC DOENS'T START UNTIL MOUSE PRESS
		if (mousePressed) {
			song.loop();
		}

		pushMatrix(); // save off 3d frame of reference before painting 2d
		hint(DISABLE_DEPTH_TEST); // switch off Z (depth) axis

		drawMusic();

		hint(ENABLE_DEPTH_TEST); // restore Z (depth) axis
		popMatrix(); // restore 3d frame of reference

		// update the cam
		if (kinect != null) {

			kinect.update();
			
			/*// This doesn't work yet.
			if(getUsersSensed()) {
				song.loop();
			}
			 */
			int[] userList = kinect.getUsers();

			position3dAndScale();

			if(SHOW_DEPTH_FIELD) {
				drawPointCloud();
			}

			if(SHOW_SKELETONS) {
				drawSkeletons(userList);
			}
			
			moveMouse(userList);

		}

	}
	

	/**
	 * Set up the Kinect and ensure connectivity
	 * 
	 * @param pApplet
	 * @return true if set up successful, false otherwise
	 */
	private boolean setupKinect(PApplet pApplet) {

		println("Setting up Kinect...");

		kinect = new SimpleOpenNI(this, SimpleOpenNI.RUN_MODE_MULTI_THREADED);
		if (kinect.isInit() == false) {
			println("Can't init SimpleOpenNI, maybe the camera is not connected!");
			return false;
		}

		// disable mirror
		kinect.setMirror(false);

		// enable depthMap generation
		kinect.enableDepth();

		// enable skeleton generation for all joints
		kinect.enableUser();

		return true;
	}
	
	/**
	 * If users sensed at some point then set to true.
	 * @return
	 */
	boolean getUsersSensed() {
		if(usersSensed) {
			return usersSensed;
		}
		if(kinect!=null && kinect.getUsers().length > 0) {
			usersSensed = true;
		}
		return false;
	}

	float getJointAngle(int userId, int jointID1, int jointID2) {
		PVector joint1 = new PVector();
		PVector joint2 = new PVector();
		kinect.getJointPositionSkeleton(userId, jointID1, joint1);
		kinect.getJointPositionSkeleton(userId, jointID2, joint2);
		return atan2(joint1.y - joint2.y, joint1.x - joint2.x);
	}

	void position3dAndScale() {
		// set the scene pos
		translate(width / 2, height / 2, 0);
		rotateX(rotX);
		rotateY(rotY);
		scale(zoomF);

		translate(0, 0, -1000); // set the rotation center of the scene 1000
								// infront of the camera
	}

	void drawPointCloud() {
		// draw the 3d point depth map
		int[] depthMap = kinect.depthMap();
		int steps = 10; // to speed up the drawing, draw every third point
		int index;
		PVector realWorldPoint;

		// draw point cloud
		stroke(200);
		beginShape(POINTS);
		for (int y = 0; y < kinect.depthHeight(); y += steps) {
			for (int x = 0; x < kinect.depthWidth(); x += steps) {
				index = x + y * kinect.depthWidth();
				if (depthMap[index] > 0) {
					// draw the projected point
					realWorldPoint = kinect.depthMapRealWorld()[index];
					vertex(realWorldPoint.x, realWorldPoint.y, realWorldPoint.z);
				}
			}
		}
		endShape();

	}

	void drawSkeletons(int[] userList) {
		// draw the skeleton if it's available

		for (int i = 0; i < userList.length; i++) {
			if (kinect.isTrackingSkeleton(userList[i]))
				drawSkeleton(userList[i]);

			// draw the center of mass
			if (kinect.getCoM(userList[i], com)) {
				stroke(100, 255, 0);
				strokeWeight(1);
				beginShape(LINES);
				vertex(com.x - 15, com.y, com.z);
				vertex(com.x + 15, com.y, com.z);

				vertex(com.x, com.y - 15, com.z);
				vertex(com.x, com.y + 15, com.z);

				vertex(com.x, com.y, com.z - 15);
				vertex(com.x, com.y, com.z + 15);
				endShape();

				fill(0, 255, 100);
				text(Integer.toString(userList[i]), com.x, com.y, com.z);

			}
		}
	}

	void moveMouse(int[] userList) {
		// PVector jointPosition = new PVector();
		// float confidence = 0;

		if (robot == null) {
			println("Robot class not initialized.  Unable to move mouse");
			return;
		}

		if (userList.length > 0) {
			// get first user
			PVector handPos = new PVector();
			kinect.getJointPositionSkeleton(userList[0], SimpleOpenNI.SKEL_LEFT_HAND, handPos);
			PVector convertedHandPos = new PVector();
			kinect.convertRealWorldToProjective(handPos, convertedHandPos);

			kinect.getCoM(userList[0], com);

			pushMatrix();
			
			if (SHOW_MOUSE_COORDINATES) {
				translate(handPos.x, handPos.y, handPos.z);
				rotateX(rotX);
				fill(255, 255, 0);
				ellipse(0, 0, 50, 50);

				// Show X & Y current coordinates
				fill(255, 0, 0);
				textSize(64);
				text("MOUSE X: " + mouseX + "  Y: " + mouseY, 0, 0, 0);

				fill(0, 255, 0);
				text("MOUSE TO HAND: X: " + (width / 2 - Math.round(handPos.x)) + "  Y: "
						+ (height / 2 - Math.round(handPos.y)), 0, 100, 0);
			}
			robot.mouseMove((width / 2 - Math.round(handPos.x)), (height / 2 - Math.round(handPos.y)));

			popMatrix();

		}

	}

	void drawMusic() {
		// .......................................................................................

		noStroke(); // FADE BACKGROUND
		fill(0, 20); // 10);
		rectMode(CORNER);
		rect(0, 0, width, height);

		c1 = color(random(255), random(129), random(247));
		// .......................................................................................

		float[] bands = new float[32];

		/*
		 * if (v%RASTRO == 0) { background(0); }
		 */
		v++;
		fft.forward(song.mix);
		// fft.forward(in.mix);
		for (int i = 0; i < fft.specSize() / 2; i++) {
			bands[i / ((fft.specSize() / 2) / (bands.length - 1))] += fft.getBand(i);
		}
		// float total = 0.0; WHY IS THIS HERE??????? (IN THE ORIGINAL) DOING
		// NOTHING??

		// translate(width/20, height/2); //height/1.1); // MORE INTERESTING
		// WHEN IMAGE IS OFF CENTER
		float band_m0 = (bands[0] / fft.specSize() / 2);
		float band_m1 = (bands[1] / fft.specSize() / 2);
		float band_m2 = (bands[2] / fft.specSize() / 2) * 10;
		float band_m3 = (bands[3] / fft.specSize() / 2) * 10;
		float band_m4 = (bands[4] / fft.specSize() / 2) * 10;
		float band_m5 = (bands[5] / fft.specSize() / 2) * 10;

		if (bands[0] > 25) {
			strokeWeight(2);
			stroke(255, 255, 0);
			// noFill();
			fill(255 * band_m0, 180, 255, 130); // BLUE //255, 200); // AQUA //,
												// 100);
			ellipse(pmouseX, pmouseY, bands[0] * 4, bands[0] * 4); // 0, 0,
																	// bands[0]*10,
																	// bands[0]*10);
		}
		if (bands[1] > 20) {
			strokeWeight(15);
			stroke(255, 197, 80); // , 125);
			noFill();
			rotate(PI / random(5));
			triangle(-10 * bands[1], -10 * bands[1], mouseX, 10 * bands[1], 10 * bands[1], -10 * bands[1]);
			// triangle(random(-width/2, width/2), random(-height/2, height/2),
			// band_m1*28, band_m1*28,10*bands[1], -10*bands[1]);
		}
		if (bands[2] > 5) {
			strokeWeight(10);
			stroke(255 * band_m2, 100, 155); // , 75);
			noFill();
			// fill(255*band_m2, 200, 155, 75);
			rotate(PI / random(3));
			rect(mouseX, mouseY, 15 * bands[2], 15 * bands[2]);
		} /*
		 * if (bands[3] > 5) { strokeWeight(band_m3); //*50); //*100); SPIRAL
		 * BANDS stroke (0); //(255*band_m3, 255, 255); //255, 255, 255);
		 * //*band_m3, 255, 255); //255, 128); //, 100); //fill(255, 255, 255,
		 * 150); // 255*band_m3, 255, 128, 200); //70); //ellipse(0, 0,
		 * 15*bands[2], 15*bands[2]); //spiral(500*band_m3, bands[3],
		 * (int)(band_m3*2*width)); spiral(pmouseX, pmouseY,
		 * (int)(band_m3*2*width)); //50*band_m3, bands[3],
		 * (int)(band_m3*2*width)); strokeWeight(0); }
		 */
		if (bands[4] > 1) {
			strokeWeight(3);
			stroke(255, 0, 0);
			fill(c1); // 255, 129, 247, 180); // ROSE-COLORED SQUARES //
						// (255*band_m4, 255, 255, 200);
			rect(random(width / 2), random(height / 2), band_m4 * 280, band_m4 * 280); // random(-width/2,
																						// width/2),
																						// random(-height/2,
																						// height/2),
																						// band_m4*280,
																						// band_m4*280);
		}
		if (bands[5] > 0.5) {
			rectMode(CENTER);
			strokeWeight(2);
			stroke(0);
			fill(254, 255, 9); // YELLOW SQUARES //255*band_m5);
			rect(width / 2, height / 2, band_m5 * 250, band_m5 * 250); // random(-width/2,
																		// width/2),
																		// random(-height/2,
																		// height/2),
																		// band_m5*250,
																		// band_m5*250);
			rectMode(CORNER);
		}
	}

	// draw the skeleton with the selected joints
	public void drawSkeleton(int userId) {
		strokeWeight(3);

		// to get the 3d joint data
		drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

		drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
		drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
		drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

		drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
		drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

		drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
		drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

		drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
		drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
		drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

		drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
		drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
		drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);

		// draw body direction
		getBodyDirection(userId, bodyCenter, bodyDir);

		bodyDir.mult(200); // 200mm length
		bodyDir.add(bodyCenter);

		stroke(255, 200, 200);
		line(bodyCenter.x, bodyCenter.y, bodyCenter.z, bodyDir.x, bodyDir.y, bodyDir.z);

		strokeWeight(1);

	}

	public void drawLimb(int userId, int jointType1, int jointType2) {
		PVector jointPos1 = new PVector();
		PVector jointPos2 = new PVector();
		float confidence;

		// draw the joint position
		confidence = kinect.getJointPositionSkeleton(userId, jointType1, jointPos1);
		confidence = kinect.getJointPositionSkeleton(userId, jointType2, jointPos2);

		stroke(255, 0, 0, confidence * 200 + 55);
		line(jointPos1.x, jointPos1.y, jointPos1.z, jointPos2.x, jointPos2.y, jointPos2.z);

		drawJointOrientation(userId, jointType1, jointPos1, 50);
	}

	public void drawJointOrientation(int userId, int jointType, PVector pos, float length) {
		// draw the joint orientation
		PMatrix3D orientation = new PMatrix3D();
		float confidence = kinect.getJointOrientationSkeleton(userId, jointType, orientation);
		if (confidence < 0.001f)
			// nothing to draw, orientation data is useless
			return;

		pushMatrix();
		translate(pos.x, pos.y, pos.z);

		// set the local coordsys
		applyMatrix(orientation);

		// coordsys lines are 100mm long
		// x - r
		stroke(255, 0, 0, confidence * 200 + 55);
		line(0, 0, 0, length, 0, 0);
		// y - g
		stroke(0, 255, 0, confidence * 200 + 55);
		line(0, 0, 0, 0, length, 0);
		// z - b
		stroke(0, 0, 255, confidence * 200 + 55);
		line(0, 0, 0, 0, 0, length);
		popMatrix();
	}

	public void getBodyDirection(int userId, PVector centerPoint, PVector dir) {
		PVector jointL = new PVector();
		PVector jointH = new PVector();
		PVector jointR = new PVector();
		float confidence;

		// draw the joint position
		confidence = kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, jointL);
		confidence = kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_HEAD, jointH);
		confidence = kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, jointR);

		// take the neck as the center point
		confidence = kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_NECK, centerPoint);

		/*
		 * // manually calc the centerPoint PVector shoulderDist =
		 * PVector.sub(jointL,jointR);
		 * centerPoint.set(PVector.mult(shoulderDist,.5));
		 * centerPoint.add(jointR);
		 */

		PVector up = PVector.sub(jointH, centerPoint);
		PVector left = PVector.sub(jointR, centerPoint);

		dir.set(up.cross(left));
		dir.normalize();
	}

	void spiral(float segments, float N_voltas, int raio_max) {
		int t, raio;
		float cx, cy, px0 = 0, py0 = 0, px1 = 0, py1 = 0;
		float angle;
		for (t = 0; t < segments; t++) {
			cx = 0;
			cy = 0;
			raio = (int) ((t / segments) * raio_max);
			angle = (float) ((t / segments) * 2 * 3.1415 * N_voltas);
			px1 = cx + raio * cos(angle);
			py1 = cy + raio * sin(angle);
			line(px0, py0, px1, py1);
			px0 = px1;
			py0 = py1;
		}
		// saveFrame("frames/lines-#######.tif"); THIS IS FOR MAKING A VIDEO
		// WITH MOVIE MAKER
	}

	public void stop() {
		in.close();
		minim.stop();

		super.stop();
	}

	// -----------------------------------------------------------------
	// SimpleOpenNI user events

	public void onNewUser(SimpleOpenNI curContext, int userId) {
		println("onNewUser - userId: " + userId);
		println("\tstart tracking skeleton");

		kinect.startTrackingSkeleton(userId);
	}

	public void onLostUser(SimpleOpenNI curContext, int userId) {
		println("onLostUser - userId: " + userId);
	}

	public void onVisibleUser(SimpleOpenNI curContext, int userId) {
		// println("onVisibleUser - userId: " + userId);
	}

	public void keyPressed() {
		exit();
	}
}